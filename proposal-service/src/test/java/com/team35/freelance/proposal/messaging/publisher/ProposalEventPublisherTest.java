// Create: proposal-service/src/test/java/com/team35/freelance/proposal/messaging/publisher/ProposalEventPublisherTest.java
package com.team35.freelance.proposal.messaging.publisher;

import com.team35.freelance.contracts.events.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    ProposalEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ProposalEventPublisher(rabbitTemplate);
    }

    @Test
    void publishAccepted_sendsToCorrectExchangeAndRoutingKey() {
        ProposalAcceptedEvent event = new ProposalAcceptedEvent(1L, 10L, 5L, BigDecimal.valueOf(2000));
        publisher.publishAccepted(event);

        verify(rabbitTemplate).convertAndSend("proposal.events", "proposal.accepted", event);
    }

    @Test
    void publishCompleted_sendsToCorrectExchangeAndRoutingKey() {
        ProposalCompletedEvent event = new ProposalCompletedEvent(1L, 10L, 5L, 99L, BigDecimal.valueOf(2000));
        publisher.publishCompleted(event);

        verify(rabbitTemplate).convertAndSend("proposal.events", "proposal.completed", event);
    }

    @Test
    void publishCancelled_sendsToCorrectExchangeAndRoutingKey() {
        ProposalCancelledEvent event = new ProposalCancelledEvent(1L, 10L, 5L, "payment_failed");
        publisher.publishCancelled(event);

        verify(rabbitTemplate).convertAndSend("proposal.events", "proposal.cancelled", event);
    }

    @Test
    void publishWithdrawn_sendsToCorrectExchangeAndRoutingKey() {
        ProposalWithdrawnEvent event = new ProposalWithdrawnEvent(1L, 10L, 5L);
        publisher.publishWithdrawn(event);

        verify(rabbitTemplate).convertAndSend("proposal.events", "proposal.withdrawn", event);
    }

    @Test
    void publishAccepted_rabbitTemplateThrows_exceptionPropagates() {
        ProposalAcceptedEvent event = new ProposalAcceptedEvent(1L, 10L, 5L, BigDecimal.valueOf(2000));
        doThrow(new RuntimeException("rabbit down"))
                .when(rabbitTemplate).convertAndSend(any(), any(String.class), any(Object.class));

        // Publisher doesn't swallow exceptions — caller handles them
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                publisher.publishAccepted(event)
        );
    }

    @Test
    void publishCancelled_correctPayloadFields() {
        ProposalCancelledEvent event = new ProposalCancelledEvent(42L, 10L, 5L, "payout_abandoned");
        publisher.publishCancelled(event);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("proposal.events"), eq("proposal.cancelled"), payloadCaptor.capture());

        ProposalCancelledEvent captured = (ProposalCancelledEvent) payloadCaptor.getValue();
        assertThat(captured.proposalId()).isEqualTo(42L);
        assertThat(captured.reason()).isEqualTo("payout_abandoned");
    }
}