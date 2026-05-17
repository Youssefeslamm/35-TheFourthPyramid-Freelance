package com.team35.freelance.wallet.messaging.publisher;

import com.team35.freelance.contracts.events.PaymentCompletedEvent;
import com.team35.freelance.contracts.events.PaymentFailedEvent;
import com.team35.freelance.contracts.events.PaymentInitiatedEvent;
import com.team35.freelance.contracts.events.PaymentRefundedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final String EXCHANGE = "payment.events";

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishInitiated(PaymentInitiatedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.initiated", event);
    }

    public void publishCompleted(PaymentCompletedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.completed", event);
    }

    public void publishFailed(PaymentFailedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.failed", event);
    }

    public void publishRefunded(PaymentRefundedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "payment.refunded", event);
    }
}

