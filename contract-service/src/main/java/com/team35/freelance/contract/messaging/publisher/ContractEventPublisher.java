package com.team35.freelance.contract.messaging.publisher;

import com.team35.freelance.contracts.events.ContractCancelledEvent;
import com.team35.freelance.contracts.events.ContractCreatedEvent;
import com.team35.freelance.contracts.events.ContractStatusChangedEvent;
import com.team35.freelance.contracts.observability.RabbitObservability;
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
        publish("contract.created", event, "contractId", event.contractId());
    }

    public void publishStatusChanged(ContractStatusChangedEvent event) {
        publish("contract.status-changed", event, "contractId", event.contractId());
    }

    public void publishCancelled(ContractCancelledEvent event) {
        publish("contract.cancelled", event, "contractId", event.contractId());
    }

    private void publish(String routingKey, Object payload, String entityKey, Long entityValue) {
        RabbitObservability.publish(routingKey, entityKey, entityValue,
                () -> rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload));
    }
}
