package com.team35.freelance.user.messaging.publisher;

import com.team35.freelance.contracts.events.UserDeactivatedEvent;
import com.team35.freelance.contracts.events.UserRegisteredEvent;
import com.team35.freelance.contracts.observability.RabbitObservability;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher {

    private static final String EXCHANGE = "user.events";

    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        publish("user.registered", event, "userId", event.userId());
    }

    public void publishUserDeactivated(UserDeactivatedEvent event) {
        publish("user.deactivated", event, "userId", event.userId());
    }

    private void publish(String routingKey, Object payload, String entityKey, Long entityValue) {
        RabbitObservability.publish(routingKey, entityKey, entityValue,
                () -> rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload));
    }
}
