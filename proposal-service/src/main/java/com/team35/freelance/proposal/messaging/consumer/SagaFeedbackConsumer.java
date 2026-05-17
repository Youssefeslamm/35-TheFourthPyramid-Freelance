package com.team35.freelance.proposal.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team35.freelance.contracts.events.ContractCancelledEvent;
import com.team35.freelance.contracts.events.ContractCreatedEvent;
import com.team35.freelance.contracts.events.ContractStatusChangedEvent;
import com.team35.freelance.contracts.events.JobClosedEvent;
import com.team35.freelance.contracts.events.PaymentCompletedEvent;
import com.team35.freelance.contracts.events.PaymentFailedEvent;
import com.team35.freelance.contracts.events.PaymentInitiatedEvent;
import com.team35.freelance.contracts.events.PaymentRefundedEvent;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.UserDeactivatedEvent;
import com.team35.freelance.contracts.observability.RabbitObservability;
import com.team35.freelance.proposal.config.ProposalRabbitConfig;
import com.team35.freelance.proposal.messaging.publisher.ProposalEventPublisher;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import com.team35.freelance.proposal.repository.ProposalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SagaFeedbackConsumer {

    private static final Logger log = LoggerFactory.getLogger(SagaFeedbackConsumer.class);

    private final ProposalRepository proposalRepository;
    private final ProposalEventPublisher proposalEventPublisher;
    private final ObjectMapper objectMapper;

    public SagaFeedbackConsumer(ProposalRepository proposalRepository,
                                ProposalEventPublisher proposalEventPublisher,
                                ObjectMapper objectMapper) {
        this.proposalRepository = proposalRepository;
        this.proposalEventPublisher = proposalEventPublisher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = ProposalRabbitConfig.SAGA_FEEDBACK_QUEUE)
    @Transactional
    public void onSagaFeedback(Message message,
                               @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) throws Exception {
        RabbitObservability.applyInboundMdc(message);
        try {
            switch (routingKey) {
                case "contract.created" -> {
                    ContractCreatedEvent event = readEvent(message, ContractCreatedEvent.class);
                    putContractMdc(event.proposalId(), event.contractId());
                    RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());
                    onContractCreated(event);
                    RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                }
                case "contract.status-changed" -> {
                    ContractStatusChangedEvent event = readEvent(message, ContractStatusChangedEvent.class);
                    putContractMdc(null, event.contractId());
                    RabbitObservability.logConsuming(routingKey, "contractId", event.contractId());
                    onContractStatusChanged(event);
                    RabbitObservability.logProcessed(routingKey, "contractId", event.contractId());
                }
                case "contract.cancelled" -> {
                    ContractCancelledEvent event = readEvent(message, ContractCancelledEvent.class);
                    putContractMdc(event.proposalId(), event.contractId());
                    RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());
                    onContractCancelled(event);
                    RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                }
                case "payment.initiated" -> {
                    PaymentInitiatedEvent event = readEvent(message, PaymentInitiatedEvent.class);
                    putPaymentMdc(event.proposalId(), event.payoutId());
                    RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());
                    onPaymentInitiated(event);
                    RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                }
                case "payment.completed" -> {
                    PaymentCompletedEvent event = readEvent(message, PaymentCompletedEvent.class);
                    putPaymentMdc(event.proposalId(), event.payoutId());
                    RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());
                    onPaymentCompleted(event);
                    RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                }
                case "payment.failed" -> {
                    PaymentFailedEvent event = readEvent(message, PaymentFailedEvent.class);
                    putPaymentMdc(event.proposalId(), event.payoutId());
                    RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());
                    onPaymentFailed(event);
                    RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                }
                case "payment.refunded" -> {
                    PaymentRefundedEvent event = readEvent(message, PaymentRefundedEvent.class);
                    putPaymentMdc(event.proposalId(), event.payoutId());
                    RabbitObservability.logConsuming(routingKey, "proposalId", event.proposalId());
                    onPaymentRefunded(event);
                    RabbitObservability.logProcessed(routingKey, "proposalId", event.proposalId());
                }
                case "job.closed" -> {
                    JobClosedEvent event = readEvent(message, JobClosedEvent.class);
                    MDC.put("jobId", String.valueOf(event.jobId()));
                    RabbitObservability.logConsuming(routingKey, "jobId", event.jobId());
                    onJobClosed(event);
                    RabbitObservability.logProcessed(routingKey, "jobId", event.jobId());
                }
                case "user.deactivated" -> {
                    UserDeactivatedEvent event = readEvent(message, UserDeactivatedEvent.class);
                    MDC.put("userId", String.valueOf(event.userId()));
                    RabbitObservability.logConsuming(routingKey, "userId", event.userId());
                    onUserDeactivated(event);
                    RabbitObservability.logProcessed(routingKey, "userId", event.userId());
                }
                default -> log.warn("Ignoring unsupported saga feedback routingKey={}", routingKey);
            }
        } catch (Exception e) {
            RabbitObservability.logFailed(routingKey, e.getMessage(), e);
            throw new IllegalStateException("Failed to process saga feedback event", e);
        } finally {
            RabbitObservability.clearConsumerMdc("proposalId", "contractId", "payoutId", "jobId", "userId");
        }
    }

    private <T> T readEvent(Message message, Class<T> eventType) throws Exception {
        return objectMapper.readValue(message.getBody(), eventType);
    }

    private void putContractMdc(Long proposalId, Long contractId) {
        if (proposalId != null) {
            MDC.put("proposalId", String.valueOf(proposalId));
        }
        if (contractId != null) {
            MDC.put("contractId", String.valueOf(contractId));
        }
    }

    private void putPaymentMdc(Long proposalId, Long payoutId) {
        if (proposalId != null) {
            MDC.put("proposalId", String.valueOf(proposalId));
        }
        if (payoutId != null) {
            MDC.put("payoutId", String.valueOf(payoutId));
        }
    }

    private void onContractCreated(ContractCreatedEvent event) {
        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));
        proposal.setContractId(event.contractId());
        proposalRepository.save(proposal);
    }

    private void onContractStatusChanged(ContractStatusChangedEvent event) {
        // Observability-only: proposal status is driven by payment events.
    }

    private void onContractCancelled(ContractCancelledEvent event) {
        // Observability-only: compensation confirmed on contract-service.
    }

    private void onPaymentInitiated(PaymentInitiatedEvent event) {
        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposalRepository.save(proposal);
    }

    private void onPaymentCompleted(PaymentCompletedEvent event) {
        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));
        proposal.setStatus(ProposalStatus.PAID);
        proposalRepository.save(proposal);
    }

    private void onPaymentFailed(PaymentFailedEvent event) {
        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));
        proposal.setStatus(ProposalStatus.PAYMENT_FAILED);
        proposalRepository.save(proposal);
        proposalEventPublisher.publishCancelled(
                new ProposalCancelledEvent(
                        proposal.getId(),
                        proposal.getJobId(),
                        proposal.getFreelancerId(),
                        event.reason()
                )
        );
    }

    private void onPaymentRefunded(PaymentRefundedEvent event) {
        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));
        proposal.setStatus(ProposalStatus.REFUNDED);
        proposalRepository.save(proposal);
    }

    private void onJobClosed(JobClosedEvent event) {
        int rejected = proposalRepository.updateStatusForJobAndStatus(
                event.jobId(),
                ProposalStatus.SUBMITTED,
                ProposalStatus.REJECTED
        );
        log.info("Rejected {} submitted proposals for jobId={}", rejected, event.jobId());
    }

    private void onUserDeactivated(UserDeactivatedEvent event) {
        int withdrawn = proposalRepository.updateStatusForFreelancerAndStatus(
                event.userId(),
                ProposalStatus.SUBMITTED,
                ProposalStatus.WITHDRAWN
        );
        log.info("Withdrawn {} submitted proposals for userId={}", withdrawn, event.userId());
    }
}
