package com.team35.freelance.proposal.messaging.publisher;

import com.team35.freelance.contracts.events.ProposalAcceptedEvent;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.contracts.events.ProposalWithdrawnEvent;
import com.team35.freelance.contracts.observability.RabbitObservability;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProposalEventPublisher {

    private static final String EXCHANGE = "proposal.events";

    private final RabbitTemplate rabbitTemplate;

    public ProposalEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishAccepted(ProposalAcceptedEvent event) {
        publish("proposal.accepted", event, "proposalId", event.proposalId());
    }

    public void publishCompleted(ProposalCompletedEvent event) {
        publish("proposal.completed", event, "proposalId", event.proposalId());
    }

    public void publishCancelled(ProposalCancelledEvent event) {
        publish("proposal.cancelled", event, "proposalId", event.proposalId());
    }

    public void publishWithdrawn(ProposalWithdrawnEvent event) {
        publish("proposal.withdrawn", event, "proposalId", event.proposalId());
    }

    private void publish(String routingKey, Object payload, String entityKey, Long entityValue) {
        RabbitObservability.publish(routingKey, entityKey, entityValue,
                () -> rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload));
    }
}
