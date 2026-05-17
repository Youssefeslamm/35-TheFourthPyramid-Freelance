package com.team35.freelance.contract.messaging.publisher;

import com.team35.freelance.contracts.events.ContractCancelledEvent;
import com.team35.freelance.contracts.events.ContractCreatedEvent;
import com.team35.freelance.contracts.events.ContractStatusChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContractEventPublisher {

    private static final String EXCHANGE = "contract.events";

    private final RabbitTemplate rabbitTemplate;

    public ContractEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishCreated(ContractCreatedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "contract.created", event);
    }

    public void publishStatusChanged(ContractStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "contract.status-changed", event);
    }

    public void publishCancelled(ContractCancelledEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "contract.cancelled", event);
    }
}

