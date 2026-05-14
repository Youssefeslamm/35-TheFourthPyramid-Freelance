package com.team35.freelance.proposal.messaging.publisher;

import com.team35.freelance.contracts.events.ProposalAcceptedEvent;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.contracts.events.ProposalWithdrawnEvent;
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
        rabbitTemplate.convertAndSend(EXCHANGE, "proposal.accepted", event);
    }

    public void publishCompleted(ProposalCompletedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "proposal.completed", event);
    }

    public void publishCancelled(ProposalCancelledEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "proposal.cancelled", event);
    }

    public void publishWithdrawn(ProposalWithdrawnEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "proposal.withdrawn", event);
    }
}

