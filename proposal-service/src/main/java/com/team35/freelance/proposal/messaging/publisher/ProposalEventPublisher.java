package com.team35.freelance.proposal.messaging.publisher;

import com.team35.freelance.contracts.events.ProposalAcceptedEvent;
import com.team35.freelance.contracts.events.ProposalCancelledEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.contracts.events.ProposalWithdrawnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProposalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProposalEventPublisher.class);
    private static final String EXCHANGE = "proposal.events";

    private final RabbitTemplate rabbitTemplate;

    public ProposalEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishAccepted(ProposalAcceptedEvent event) {
        String routingKey = "proposal.accepted";
        try {
            MDC.put("routingKey", routingKey);
            MDC.put("proposalId", String.valueOf(event.proposalId()));
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published {} for proposalId={}", routingKey, event.proposalId());
        } finally {
            MDC.remove("routingKey");
        }
    }

    public void publishCompleted(ProposalCompletedEvent event) {
        String routingKey = "proposal.completed";
        try {
            MDC.put("routingKey", routingKey);
            MDC.put("proposalId", String.valueOf(event.proposalId()));
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published {} for proposalId={}", routingKey, event.proposalId());
        } finally {
            MDC.remove("routingKey");
        }
    }

    public void publishCancelled(ProposalCancelledEvent event) {
        String routingKey = "proposal.cancelled";
        try {
            MDC.put("routingKey", routingKey);
            MDC.put("proposalId", String.valueOf(event.proposalId()));
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published {} for proposalId={}", routingKey, event.proposalId());
        } finally {
            MDC.remove("routingKey");
        }
    }

    public void publishWithdrawn(ProposalWithdrawnEvent event) {
        String routingKey = "proposal.withdrawn";
        try {
            MDC.put("routingKey", routingKey);
            MDC.put("proposalId", String.valueOf(event.proposalId()));
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
            log.info("Published {} for proposalId={}", routingKey, event.proposalId());
        } finally {
            MDC.remove("routingKey");
        }
    }
}
