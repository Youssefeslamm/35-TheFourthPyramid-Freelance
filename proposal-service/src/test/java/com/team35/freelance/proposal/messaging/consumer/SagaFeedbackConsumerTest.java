// proposal-service/src/test/java/com/team35/freelance/proposal/messaging/consumer/SagaFeedbackConsumerTest.java
package com.team35.freelance.proposal.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team35.freelance.contracts.events.*;
import com.team35.freelance.proposal.messaging.publisher.ProposalEventPublisher;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import com.team35.freelance.proposal.repository.ProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaFeedbackConsumerTest {

    @Mock ProposalRepository proposalRepository;
    @Mock ProposalEventPublisher proposalEventPublisher;

    SagaFeedbackConsumer consumer;
    ObjectMapper objectMapper;

    Proposal proposal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        consumer = new SagaFeedbackConsumer(proposalRepository, proposalEventPublisher, objectMapper);

        proposal = new Proposal();
        proposal.setId(1L);
        proposal.setJobId(10L);
        proposal.setFreelancerId(5L);
        proposal.setStatus(ProposalStatus.COMPLETING);
    }

    // Helper to build a RabbitMQ Message with a routing key
    private Message buildMessage(Object event, String routingKey) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(event);
            MessageProperties props = new MessageProperties();
            props.setReceivedRoutingKey(routingKey);
            return new Message(body, props);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void contractCreated_linksContractIdToProposal() throws Exception {
        when(proposalRepository.updateContractId(1L, 99L)).thenReturn(1);
        ContractCreatedEvent event = new ContractCreatedEvent(99L, 1L, 10L, 5L, BigDecimal.valueOf(2000));

        consumer.onSagaFeedback(
                buildMessage(event, "contract.created"),
                "contract.created"
        );

        verify(proposalRepository).updateContractId(1L, 99L);
    }

    @Test
    void paymentInitiated_setsPaymentPending() throws Exception {
        proposal.setStatus(ProposalStatus.COMPLETING);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(77L, 1L, 99L, BigDecimal.valueOf(2000));

        consumer.onSagaFeedback(
                buildMessage(event, "payment.initiated"),
                "payment.initiated"
        );

        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProposalStatus.PAYMENT_PENDING);
    }

    @Test
    void paymentCompleted_setsPaid() throws Exception {
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        PaymentCompletedEvent event = new PaymentCompletedEvent(77L, 1L, 99L, BigDecimal.valueOf(2000));

        consumer.onSagaFeedback(
                buildMessage(event, "payment.completed"),
                "payment.completed"
        );

        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProposalStatus.PAID);
    }

    @Test
    void paymentFailed_setsPaymentFailedAndPublishesCancelled() throws Exception {
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        PaymentFailedEvent event = new PaymentFailedEvent(77L, 1L, 99L, "insufficient_funds");

        consumer.onSagaFeedback(
                buildMessage(event, "payment.failed"),
                "payment.failed"
        );

        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProposalStatus.PAYMENT_FAILED);

        ArgumentCaptor<ProposalCancelledEvent> eventCaptor =
                ArgumentCaptor.forClass(ProposalCancelledEvent.class);
        verify(proposalEventPublisher).publishCancelled(eventCaptor.capture());
        assertThat(eventCaptor.getValue().proposalId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().reason()).isEqualTo("insufficient_funds");
    }

    @Test
    void paymentRefunded_setsRefunded() throws Exception {
        proposal.setStatus(ProposalStatus.PAYMENT_FAILED);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        PaymentRefundedEvent event = new PaymentRefundedEvent(77L, 1L, 99L, BigDecimal.valueOf(2000));

        consumer.onSagaFeedback(
                buildMessage(event, "payment.refunded"),
                "payment.refunded"
        );

        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProposalStatus.REFUNDED);
    }

    @Test
    void paymentFailed_proposalNotFound_throwsAndNothingPublished() throws Exception {
        when(proposalRepository.findById(1L)).thenReturn(Optional.empty());
        PaymentFailedEvent event = new PaymentFailedEvent(77L, 1L, 99L, "error");

        assertThrows(IllegalStateException.class, () ->
                consumer.onSagaFeedback(
                        buildMessage(event, "payment.failed"),
                        "payment.failed"
                )
        );

        verify(proposalEventPublisher, never()).publishCancelled(any());
    }

    @Test
    void unknownRoutingKey_logsAndDoesNotThrow() throws Exception {
        // Should hit the default branch, log a warning, not throw
        Message msg = buildMessage("{}", "some.unknown.key");
        consumer.onSagaFeedback(msg, "some.unknown.key");
        verify(proposalRepository, never()).findById(any());
    }
    // Add these to SagaFeedbackConsumerTest.java

    @Test
    void contractCreated_proposalNotFound_throwsAndNothingPublished() {
        when(proposalRepository.updateContractId(1L, 99L)).thenReturn(0);
        ContractCreatedEvent event = new ContractCreatedEvent(99L, 1L, 10L, 5L, BigDecimal.valueOf(2000));

        assertThrows(IllegalStateException.class, () ->
                consumer.onSagaFeedback(buildMessage(event, "contract.created"), "contract.created")
        );
    }

    @Test
    void paymentInitiated_proposalNotFound_throws() {
        when(proposalRepository.findById(1L)).thenReturn(Optional.empty());
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(77L, 1L, 99L, BigDecimal.valueOf(2000));

        assertThrows(IllegalStateException.class, () ->
                consumer.onSagaFeedback(buildMessage(event, "payment.initiated"), "payment.initiated")
        );
    }

    @Test
    void paymentCompleted_proposalNotFound_throws() {
        when(proposalRepository.findById(1L)).thenReturn(Optional.empty());
        PaymentCompletedEvent event = new PaymentCompletedEvent(77L, 1L, 99L, BigDecimal.valueOf(2000));

        assertThrows(IllegalStateException.class, () ->
                consumer.onSagaFeedback(buildMessage(event, "payment.completed"), "payment.completed")
        );
    }

    @Test
    void paymentRefunded_proposalNotFound_throws() {
        when(proposalRepository.findById(1L)).thenReturn(Optional.empty());
        PaymentRefundedEvent event = new PaymentRefundedEvent(77L, 1L, 99L, BigDecimal.valueOf(2000));

        assertThrows(IllegalStateException.class, () ->
                consumer.onSagaFeedback(buildMessage(event, "payment.refunded"), "payment.refunded")
        );
    }

    @Test
    void contractStatusChanged_consumedWithoutMutatingProposal() throws Exception {
        // contract.status-changed is observability only — should NOT save anything
        ContractStatusChangedEvent event = new ContractStatusChangedEvent(99L, "ACTIVE", "COMPLETED");

        consumer.onSagaFeedback(
                buildMessage(event, "contract.status-changed"),
                "contract.status-changed"
        );

        verify(proposalRepository, never()).findById(any());
        verify(proposalRepository, never()).save(any());
    }

    @Test
    void contractCancelled_consumedWithoutMutatingProposal() throws Exception {
        // contract.cancelled is observability only — should NOT save anything
        ContractCancelledEvent event = new ContractCancelledEvent(99L, 1L);

        consumer.onSagaFeedback(
                buildMessage(event, "contract.cancelled"),
                "contract.cancelled"
        );

        verify(proposalRepository, never()).findById(any());
        verify(proposalRepository, never()).save(any());
    }

    @Test
    void paymentFailed_publishesCancelledWithCorrectReason() throws Exception {
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);
        PaymentFailedEvent event = new PaymentFailedEvent(77L, 1L, 99L, "card_declined");

        consumer.onSagaFeedback(
                buildMessage(event, "payment.failed"),
                "payment.failed"
        );

        ArgumentCaptor<ProposalCancelledEvent> captor =
                ArgumentCaptor.forClass(ProposalCancelledEvent.class);
        verify(proposalEventPublisher).publishCancelled(captor.capture());
        assertThat(captor.getValue().reason()).isEqualTo("card_declined");
        assertThat(captor.getValue().jobId()).isEqualTo(proposal.getJobId());
        assertThat(captor.getValue().freelancerId()).isEqualTo(proposal.getFreelancerId());
    }

    @Test
    void contractCreated_setsCorrectContractId() throws Exception {
        when(proposalRepository.updateContractId(1L, 555L)).thenReturn(1);
        ContractCreatedEvent event = new ContractCreatedEvent(555L, 1L, 10L, 5L, BigDecimal.valueOf(2000));

        consumer.onSagaFeedback(
                buildMessage(event, "contract.created"),
                "contract.created"
        );

        verify(proposalRepository).updateContractId(1L, 555L);
    }
    // ── Idempotency tests ──────────────────────────────────────────────────────

    @Test
    void paymentCompleted_alreadyPaid_stillSavesAndDoesNotThrow() throws Exception {
        proposal.setStatus(ProposalStatus.PAID); // already PAID
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);
        PaymentCompletedEvent event = new PaymentCompletedEvent(77L, 1L, 99L, BigDecimal.valueOf(2000));

        // Should not throw — consumer is idempotent
        consumer.onSagaFeedback(buildMessage(event, "payment.completed"), "payment.completed");

        verify(proposalRepository).save(any());
    }

    @Test
    void paymentInitiated_alreadyPaymentPending_stillSaves() throws Exception {
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING); // already pending
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(77L, 1L, 99L, BigDecimal.valueOf(2000));

        consumer.onSagaFeedback(buildMessage(event, "payment.initiated"), "payment.initiated");

        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProposalStatus.PAYMENT_PENDING);
    }

    @Test
    void contractCreated_contractIdAlreadySet_isIdempotentNotOverwritten() throws Exception {
        when(proposalRepository.updateContractId(1L, 11L)).thenReturn(1);
        ContractCreatedEvent event = new ContractCreatedEvent(11L, 1L, 10L, 5L, BigDecimal.valueOf(2000));

        // Same contractId — idempotent
        consumer.onSagaFeedback(buildMessage(event, "contract.created"), "contract.created");

        verify(proposalRepository).updateContractId(1L, 11L);
    }

    @Test
    void paymentFailed_alreadyPaymentFailed_stillPublishesCancelled() throws Exception {
        proposal.setStatus(ProposalStatus.PAYMENT_FAILED); // already failed
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);
        PaymentFailedEvent event = new PaymentFailedEvent(77L, 1L, 99L, "retry_failed");

        consumer.onSagaFeedback(buildMessage(event, "payment.failed"), "payment.failed");

        verify(proposalEventPublisher).publishCancelled(any());
    }

// ── Payload completeness tests ─────────────────────────────────────────────

    @Test
    void paymentFailed_cancelledEvent_hasAllFields() throws Exception {
        proposal.setStatus(ProposalStatus.PAYMENT_PENDING);
        proposal.setJobId(42L);
        proposal.setFreelancerId(7L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);
        PaymentFailedEvent event = new PaymentFailedEvent(77L, 1L, 99L, "bank_rejected");

        consumer.onSagaFeedback(buildMessage(event, "payment.failed"), "payment.failed");

        ArgumentCaptor<ProposalCancelledEvent> captor =
                ArgumentCaptor.forClass(ProposalCancelledEvent.class);
        verify(proposalEventPublisher).publishCancelled(captor.capture());
        assertThat(captor.getValue().proposalId()).isEqualTo(1L);
        assertThat(captor.getValue().jobId()).isEqualTo(42L);
        assertThat(captor.getValue().freelancerId()).isEqualTo(7L);
        assertThat(captor.getValue().reason()).isEqualTo("bank_rejected");
    }

    @Test
    void paymentRefunded_setsRefunded_verifyNothingElsePublished() throws Exception {
        proposal.setStatus(ProposalStatus.PAYMENT_FAILED);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);
        PaymentRefundedEvent event = new PaymentRefundedEvent(77L, 1L, 99L, BigDecimal.valueOf(500));

        consumer.onSagaFeedback(buildMessage(event, "payment.refunded"), "payment.refunded");

        verify(proposalEventPublisher, never()).publishCancelled(any());
        verify(proposalEventPublisher, never()).publishCompleted(any());
        verify(proposalEventPublisher, never()).publishAccepted(any());
        verify(proposalEventPublisher, never()).publishWithdrawn(any());
    }

    @Test
    void contractStatusChanged_doesNotInteractWithRepository() throws Exception {
        ContractStatusChangedEvent event = new ContractStatusChangedEvent(99L, "ACTIVE", "COMPLETED");
        consumer.onSagaFeedback(buildMessage(event, "contract.status-changed"), "contract.status-changed");
        verifyNoInteractions(proposalRepository);
        verifyNoInteractions(proposalEventPublisher);
    }

    @Test
    void multipleUnknownRoutingKeys_allIgnoredGracefully() throws Exception {
        for (String key : new String[]{"user.registered", "random.key", ""}) {
            consumer.onSagaFeedback(buildMessage("{}", key), key);
        }
        verifyNoInteractions(proposalRepository);
        verifyNoInteractions(proposalEventPublisher);
    }

    @Test
    void jobClosed_rejectsSubmittedProposalsForJob() throws Exception {
        JobClosedEvent event = new JobClosedEvent(10L, 3L);
        when(proposalRepository.updateStatusForJobAndStatus(
                10L,
                ProposalStatus.SUBMITTED,
                ProposalStatus.REJECTED
        )).thenReturn(2);

        consumer.onSagaFeedback(buildMessage(event, "job.closed"), "job.closed");

        verify(proposalRepository).updateStatusForJobAndStatus(
                10L,
                ProposalStatus.SUBMITTED,
                ProposalStatus.REJECTED
        );
        verify(proposalRepository, never()).save(any());
    }

    @Test
    void userDeactivated_withdrawsSubmittedProposalsForFreelancer() throws Exception {
        UserDeactivatedEvent event = new UserDeactivatedEvent(5L);
        when(proposalRepository.updateStatusForFreelancerAndStatus(
                5L,
                ProposalStatus.SUBMITTED,
                ProposalStatus.WITHDRAWN
        )).thenReturn(3);

        consumer.onSagaFeedback(buildMessage(event, "user.deactivated"), "user.deactivated");

        verify(proposalRepository).updateStatusForFreelancerAndStatus(
                5L,
                ProposalStatus.SUBMITTED,
                ProposalStatus.WITHDRAWN
        );
        verify(proposalRepository, never()).save(any());
    }
}
