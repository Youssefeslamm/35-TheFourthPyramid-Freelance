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
    private Message buildMessage(Object event, String routingKey) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(event);
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey(routingKey);
        return new Message(body, props);
    }

    @Test
    void contractCreated_linksContractIdToProposal() throws Exception {
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        ContractCreatedEvent event = new ContractCreatedEvent(99L, 1L, 10L, 5L, BigDecimal.valueOf(2000));

        consumer.onSagaFeedback(
                buildMessage(event, "contract.created"),
                "contract.created"
        );

        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(captor.capture());
        assertThat(captor.getValue().getContractId()).isEqualTo(99L);
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
        when(proposalRepository.findById(1L)).thenReturn(Optional.empty());
        ContractCreatedEvent event = new ContractCreatedEvent(99L, 1L, 10L, 5L, BigDecimal.valueOf(2000));

        assertThrows(IllegalStateException.class, () ->
                consumer.onSagaFeedback(buildMessage(event, "contract.created"), "contract.created")
        );
        verify(proposalRepository, never()).save(any());
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
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenReturn(proposal);
        ContractCreatedEvent event = new ContractCreatedEvent(555L, 1L, 10L, 5L, BigDecimal.valueOf(2000));

        consumer.onSagaFeedback(
                buildMessage(event, "contract.created"),
                "contract.created"
        );

        ArgumentCaptor<Proposal> captor = ArgumentCaptor.forClass(Proposal.class);
        verify(proposalRepository).save(captor.capture());
        assertThat(captor.getValue().getContractId()).isEqualTo(555L);
    }
}