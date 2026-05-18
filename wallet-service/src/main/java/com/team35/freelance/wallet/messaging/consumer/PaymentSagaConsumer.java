package com.team35.freelance.wallet.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team35.freelance.contracts.events.PaymentInitiatedEvent;
import com.team35.freelance.contracts.events.PaymentRefundedEvent;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.contracts.feign.UserServiceClient;
import com.team35.freelance.contracts.observability.RabbitObservability;
import com.team35.freelance.wallet.messaging.publisher.PaymentEventPublisher;
import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutMethod;
import com.team35.freelance.wallet.model.PayoutStatus;
import com.team35.freelance.wallet.repository.PayoutRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentSagaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaConsumer.class);

    private final PayoutRepository payoutRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    public PaymentSagaConsumer(PayoutRepository payoutRepository,
                               PaymentEventPublisher paymentEventPublisher,
                               UserServiceClient userServiceClient,
                               ObjectMapper objectMapper) {
        this.payoutRepository = payoutRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.userServiceClient = userServiceClient;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payment.saga-listener")
    public void handleSagaEvent(Message message,
                                @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) throws Exception {
        RabbitObservability.applyInboundMdc(message);
        try {
            switch (routingKey) {
                case "proposal.completed" -> {
                    ProposalCompletedEvent event = objectMapper.readValue(message.getBody(), ProposalCompletedEvent.class);
                    putProposalMdc(event);
                    RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());
                    handleProposalCompleted(event);
                    RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                }
                case "proposal.cancelled" -> {
                    ProposalCancelledEvent event = objectMapper.readValue(message.getBody(), ProposalCancelledEvent.class);
                    putProposalMdc(event.proposalId(), event.jobId(), event.freelancerId(), null);
                    RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());
                    handleProposalCancelled(event);
                    RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                }
                case "payment.initiated" -> {
                    PaymentInitiatedEvent event = objectMapper.readValue(message.getBody(), PaymentInitiatedEvent.class);
                    putPayoutMdc(event.payoutId(), event.proposalId(), event.contractId());
                    RabbitObservability.logConsuming(routingKey, "payoutId", event.payoutId());
                    log.info("Payment initiated event received payoutId={}", event.payoutId());
                    RabbitObservability.logProcessed(routingKey, "payoutId", event.payoutId());
                }
                default -> throw new IllegalArgumentException("Unsupported routing key: " + routingKey);
            }
        } catch (Exception e) {
            RabbitObservability.logFailed(routingKey, e.getMessage(), e);
            throw e;
        } finally {
            RabbitObservability.clearConsumerMdc("proposalId", "jobId", "userId", "contractId", "payoutId");
        }
    }

    public void handleProposalCompleted(ProposalCompletedEvent event) {
        try {
            userServiceClient.getUserById(event.freelancerId());
            log.info("Freelancer validated: {}", event.freelancerId());
        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Freelancer not found: " + event.freelancerId());
        } catch (FeignException.Unauthorized e) {
            log.error("Unauthorized call to user-service. Check Feign config / headers");
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in saga", e);
            throw e;
        }

        Payout existing = payoutRepository.findByContractId(event.contractId());
        if (existing != null) {
            log.info("Payout already exists for contract {}, skipping duplicate proposal.completed", event.contractId());
            return;
        }

        Payout payout = new Payout();
        payout.setContractId(event.contractId());
        payout.setFreelancerId(event.freelancerId());
        payout.setAmount(event.agreedAmount().doubleValue());
        payout.setStatus(PayoutStatus.PENDING);
        payout.setMethod(PayoutMethod.BANK_TRANSFER);
        payout.setCreatedAt(LocalDateTime.now());
        payout.setProposalId(event.proposalId());

        Payout savedPayout = payoutRepository.save(payout);
        MDC.put("payoutId", String.valueOf(savedPayout.getId()));

        paymentEventPublisher.publishInitiated(
                new PaymentInitiatedEvent(
                        savedPayout.getId(),
                        event.proposalId(),
                        event.contractId(),
                        event.agreedAmount()
                )
        );
    }

    public void handleProposalCancelled(ProposalCancelledEvent event) {
        List<Payout> payouts = payoutRepository.findByFreelancerIdAndStatusIn(
                event.freelancerId(),
                List.of(PayoutStatus.PENDING, PayoutStatus.COMPLETED)
        );

        log.info("Found {} payouts to refund for freelancer {}", payouts.size(), event.freelancerId());

        for (Payout payout : payouts) {
            log.info("Refunding payout id: {}", payout.getId());
            payout.setStatus(PayoutStatus.REFUNDED);
            Payout savedPayout = payoutRepository.save(payout);
            MDC.put("payoutId", String.valueOf(savedPayout.getId()));

            paymentEventPublisher.publishRefunded(
                    new PaymentRefundedEvent(
                            savedPayout.getId(),
                            event.proposalId(),
                            savedPayout.getContractId(),
                            BigDecimal.valueOf(savedPayout.getAmount())
                    )
            );
        }
    }

    private void putProposalMdc(ProposalCompletedEvent event) {
        putProposalMdc(event.proposalId(), event.jobId(), event.freelancerId(), event.contractId());
    }

    private void putProposalMdc(Long proposalId, Long jobId, Long freelancerId, Long contractId) {
        if (proposalId != null) {
            MDC.put("proposalId", String.valueOf(proposalId));
        }
        if (jobId != null) {
            MDC.put("jobId", String.valueOf(jobId));
        }
        if (freelancerId != null) {
            MDC.put("userId", String.valueOf(freelancerId));
        }
        if (contractId != null) {
            MDC.put("contractId", String.valueOf(contractId));
        }
    }

    private void putPayoutMdc(Long payoutId, Long proposalId, Long contractId) {
        if (payoutId != null) {
            MDC.put("payoutId", String.valueOf(payoutId));
        }
        if (proposalId != null) {
            MDC.put("proposalId", String.valueOf(proposalId));
        }
        if (contractId != null) {
            MDC.put("contractId", String.valueOf(contractId));
        }
    }
}
