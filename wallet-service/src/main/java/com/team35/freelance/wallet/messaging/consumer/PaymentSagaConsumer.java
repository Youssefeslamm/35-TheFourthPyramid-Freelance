package com.team35.freelance.wallet.messaging.consumer;

import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.wallet.messaging.publisher.PaymentEventPublisher;
import com.team35.freelance.wallet.repository.PayoutRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.team35.freelance.contracts.events.PaymentInitiatedEvent;
import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutMethod;
import com.team35.freelance.wallet.model.PayoutStatus;
import com.team35.freelance.contracts.events.PaymentRefundedEvent;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;

import com.team35.freelance.contracts.feign.UserServiceClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PaymentSagaConsumer {
    private final PayoutRepository payoutRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final UserServiceClient userServiceClient;
    private static final Logger log = LoggerFactory.getLogger(PaymentSagaConsumer.class);

    public PaymentSagaConsumer(PayoutRepository payoutRepository,
                               PaymentEventPublisher paymentEventPublisher,
                               UserServiceClient userServiceClient) {
        this.payoutRepository = payoutRepository;
        this.paymentEventPublisher = paymentEventPublisher;
        this.userServiceClient = userServiceClient;
    }
    @RabbitListener(queues = "payment.saga-listener")
    public void handleSagaEvent(
            org.springframework.amqp.core.Message message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey
    ) throws Exception {

        String json = new String(message.getBody());

        log.info("Received routing key: {}", routingKey);
        log.info("Payload: {}", json);

        switch (routingKey) {

            case "proposal.completed" -> {
                ProposalCompletedEvent event =
                        new ObjectMapper().readValue(json, ProposalCompletedEvent.class);

                handleProposalCompleted(event);
            }

            case "proposal.cancelled" -> {
                ProposalCancelledEvent event =
                        new ObjectMapper().readValue(json, ProposalCancelledEvent.class);

                handleProposalCancelled(event);
            }

            case "payment.initiated" -> {
                PaymentInitiatedEvent event =
                        new ObjectMapper().readValue(json, PaymentInitiatedEvent.class);

                log.info("Payment initiated event received: {}", event);
            }

            default -> throw new IllegalArgumentException("Unsupported routing key: " + routingKey);
        }
    }
    public void handleProposalCompleted(ProposalCompletedEvent event) {

        try {
            userServiceClient.getUserById(event.freelancerId());

            // continue flow
            log.info("Freelancer validated: {}", event.freelancerId());

        } catch (FeignException.NotFound e) {
            throw new IllegalArgumentException("Freelancer not found: " + event.freelancerId());

        } catch (FeignException.Unauthorized e) {
            log.error("❌ Unauthorized call to user-service. Check Feign config / headers");
            throw e;

        } catch (Exception e) {
            log.error("❌ Unexpected error in saga", e);
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
        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("proposalId", event.proposalId());
        payout.setTransactionDetails(transactionDetails);

        payout.setCreatedAt(LocalDateTime.now());

        Payout savedPayout = payoutRepository.save(payout);

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

}
