package com.team35.freelance.proposal.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team35.freelance.contracts.events.ContractCreatedEvent;
import com.team35.freelance.contracts.events.ContractStatusChangedEvent;
import com.team35.freelance.contracts.events.JobClosedEvent;
import com.team35.freelance.contracts.events.PaymentCompletedEvent;
import com.team35.freelance.contracts.events.PaymentFailedEvent;
import com.team35.freelance.contracts.events.PaymentInitiatedEvent;
import com.team35.freelance.contracts.events.PaymentRefundedEvent;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.UserDeactivatedEvent;
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
import com.team35.freelance.contracts.events.ContractCancelledEvent;

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
                               @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        try {
            MDC.put("routingKey", routingKey);

            switch (routingKey) {
                case "contract.created" -> {
                    ContractCreatedEvent event = readEvent(message, ContractCreatedEvent.class);
                    onContractCreated(event);
                }
                case "contract.status-changed" -> {
                    ContractStatusChangedEvent event = readEvent(message, ContractStatusChangedEvent.class);
                    onContractStatusChanged(event);
                }
                case "contract.cancelled" -> {
                    ContractCancelledEvent event = readEvent(message, ContractCancelledEvent.class);
                    onContractCancelled(event);
                }
                case "payment.initiated" -> {
                    PaymentInitiatedEvent event = readEvent(message, PaymentInitiatedEvent.class);
                    onPaymentInitiated(event);
                }
                case "payment.completed" -> {
                    PaymentCompletedEvent event = readEvent(message, PaymentCompletedEvent.class);
                    onPaymentCompleted(event);
                }
                case "payment.failed" -> {
                    PaymentFailedEvent event = readEvent(message, PaymentFailedEvent.class);
                    onPaymentFailed(event);
                }
                case "payment.refunded" -> {
                    PaymentRefundedEvent event = readEvent(message, PaymentRefundedEvent.class);
                    onPaymentRefunded(event);
                }
                case "job.closed" -> {
                    JobClosedEvent event = readEvent(message, JobClosedEvent.class);
                    onJobClosed(event);
                }
                case "user.deactivated" -> {
                    UserDeactivatedEvent event = readEvent(message, UserDeactivatedEvent.class);
                    onUserDeactivated(event);
                }
                default -> log.warn("Ignoring unsupported saga feedback routingKey={}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process saga feedback routingKey={}: {}", routingKey, e.getMessage(), e);
            throw new IllegalStateException("Failed to process saga feedback event", e);
        } finally {
            MDC.remove("routingKey");
            MDC.remove("proposalId");
            MDC.remove("contractId");
            MDC.remove("payoutId");
            MDC.remove("jobId");
            MDC.remove("userId");
        }
    }

    private <T> T readEvent(Message message, Class<T> eventType) throws Exception {
        return objectMapper.readValue(message.getBody(), eventType);
    }

    private void onContractCreated(ContractCreatedEvent event) {
        MDC.put("proposalId", String.valueOf(event.proposalId()));
        MDC.put("contractId", String.valueOf(event.contractId()));

        log.info("Consuming contract.created proposalId={} contractId={}",
                event.proposalId(), event.contractId());

        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));

        proposal.setContractId(event.contractId());
        proposalRepository.save(proposal);
    }

    private void onContractStatusChanged(ContractStatusChangedEvent event) {
        MDC.put("contractId", String.valueOf(event.contractId()));

        log.info("Consuming contract.status-changed contractId={} oldStatus={} newStatus={}",
                event.contractId(), event.oldStatus(), event.newStatus());

        /*
         * Proposal status is already driven by payment events:
         * - payment.initiated  -> PAYMENT_PENDING
         * - payment.completed  -> PAID
         * - payment.failed     -> PAYMENT_FAILED + proposal.cancelled
         * - payment.refunded   -> REFUNDED
         *
         * So contract.status-changed is consumed for saga observability/correlation.
         * Do not force Proposal to PAID here, because payout may not be completed yet.
         */
    }

    // Add this method to SagaFeedbackConsumer.java:
    private void onContractCancelled(ContractCancelledEvent event) {
        MDC.put("proposalId", String.valueOf(event.proposalId()));
        MDC.put("contractId", String.valueOf(event.contractId()));

        log.info("Consuming contract.cancelled contractId={} proposalId={} — compensation confirmed on contract-service",
                event.contractId(), event.proposalId());

        // Observability only — proposal status driven by payment events.
        // This confirms contract-service has completed its compensation step.
    }

    private void onPaymentInitiated(PaymentInitiatedEvent event) {
        MDC.put("proposalId", String.valueOf(event.proposalId()));
        MDC.put("payoutId", String.valueOf(event.payoutId()));

        log.info("Consuming payment.initiated proposalId={} payoutId={}",
                event.proposalId(), event.payoutId());

        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));

        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposalRepository.save(proposal);
    }

    private void onPaymentCompleted(PaymentCompletedEvent event) {
        MDC.put("proposalId", String.valueOf(event.proposalId()));
        MDC.put("payoutId", String.valueOf(event.payoutId()));

        log.info("Consuming payment.completed proposalId={} payoutId={}",
                event.proposalId(), event.payoutId());

        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));

        proposal.setStatus(ProposalStatus.PAID);
        proposalRepository.save(proposal);
    }

    private void onPaymentFailed(PaymentFailedEvent event) {
        MDC.put("proposalId", String.valueOf(event.proposalId()));
        MDC.put("payoutId", String.valueOf(event.payoutId()));

        log.warn("Consuming payment.failed proposalId={} payoutId={} reason={}",
                event.proposalId(), event.payoutId(), event.reason());

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
        MDC.put("proposalId", String.valueOf(event.proposalId()));
        MDC.put("payoutId", String.valueOf(event.payoutId()));

        log.info("Consuming payment.refunded proposalId={} payoutId={} refundAmount={}",
                event.proposalId(), event.payoutId(), event.refundAmount());

        Proposal proposal = proposalRepository.findById(event.proposalId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proposal not found: " + event.proposalId()
                ));

        proposal.setStatus(ProposalStatus.REFUNDED);
        proposalRepository.save(proposal);
    }

    private void onJobClosed(JobClosedEvent event) {
        MDC.put("jobId", String.valueOf(event.jobId()));

        int rejected = proposalRepository.updateStatusForJobAndStatus(
                event.jobId(),
                ProposalStatus.SUBMITTED,
                ProposalStatus.REJECTED
        );

        log.info("Consuming job.closed jobId={} rejectedSubmittedProposals={}",
                event.jobId(), rejected);
    }

    private void onUserDeactivated(UserDeactivatedEvent event) {
        MDC.put("userId", String.valueOf(event.userId()));

        int withdrawn = proposalRepository.updateStatusForFreelancerAndStatus(
                event.userId(),
                ProposalStatus.SUBMITTED,
                ProposalStatus.WITHDRAWN
        );

        log.info("Consuming user.deactivated userId={} withdrawnSubmittedProposals={}",
                event.userId(), withdrawn);
    }
}
