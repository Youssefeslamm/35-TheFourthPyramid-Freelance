package com.team35.freelance.wallet.messaging.publisher;

import com.team35.freelance.contracts.events.PaymentCompletedEvent;
import com.team35.freelance.contracts.events.PaymentFailedEvent;
import com.team35.freelance.contracts.events.PaymentInitiatedEvent;
import com.team35.freelance.contracts.events.PaymentRefundedEvent;
import com.team35.freelance.contracts.observability.RabbitObservability;
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
        publish("payment.initiated", event, "payoutId", event.payoutId());
    }

    public void publishCompleted(PaymentCompletedEvent event) {
        publish("payment.completed", event, "payoutId", event.payoutId());
    }

    public void publishFailed(PaymentFailedEvent event) {
        publish("payment.failed", event, "payoutId", event.payoutId());
    }

    public void publishRefunded(PaymentRefundedEvent event) {
        publish("payment.refunded", event, "payoutId", event.payoutId());
    }

    private void publish(String routingKey, Object payload, String entityKey, Long entityValue) {
        RabbitObservability.publish(routingKey, entityKey, entityValue,
                () -> rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload));
    }
}
