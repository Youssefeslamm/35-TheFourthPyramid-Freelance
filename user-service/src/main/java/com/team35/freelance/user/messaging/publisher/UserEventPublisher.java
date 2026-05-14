package com.team35.freelance.user.messaging.publisher;

import com.team35.freelance.contracts.events.UserDeactivatedEvent;
import com.team35.freelance.contracts.events.UserRegisteredEvent;
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
        rabbitTemplate.convertAndSend(EXCHANGE, "user.registered", event);
    }

    public void publishUserDeactivated(UserDeactivatedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "user.deactivated", event);
    }
}

