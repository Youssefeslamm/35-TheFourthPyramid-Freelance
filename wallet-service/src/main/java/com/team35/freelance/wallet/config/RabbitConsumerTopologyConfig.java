package com.team35.freelance.wallet.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConsumerTopologyConfig {
    public static final String PROPOSAL_EVENTS_EXCHANGE = "proposal.events";
    public static final String PAYMENT_SAGA_QUEUE = "payment.saga-listener";
    public static final String PAYMENT_SAGA_DLQ = "payment.saga-listener.dlq";
    public static final String PAYMENT_SAGA_DLX = "payment.saga-listener.dlx";

    @Bean
    public Declarables paymentSagaListenerTopology() {
        TopicExchange proposalEventsExchange = new TopicExchange(PROPOSAL_EVENTS_EXCHANGE);
        TopicExchange deadLetterExchange = new TopicExchange(PAYMENT_SAGA_DLX);

        Queue queue = QueueBuilder.durable(PAYMENT_SAGA_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_SAGA_DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_SAGA_DLQ)
                .build();

        Queue deadLetterQueue = QueueBuilder.durable(PAYMENT_SAGA_DLQ).build();

        Binding completedBinding = BindingBuilder.bind(queue)
                .to(proposalEventsExchange)
                .with("proposal.completed");

        Binding cancelledBinding = BindingBuilder.bind(queue)
                .to(proposalEventsExchange)
                .with("proposal.cancelled");

        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(PAYMENT_SAGA_DLQ);

        return new Declarables(
                proposalEventsExchange,
                queue,
                deadLetterQueue,
                deadLetterExchange,
                completedBinding,
                cancelledBinding,
                deadLetterBinding
        );
    }
}
