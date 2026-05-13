package com.team35.freelance.user.config;

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

    @Bean
    public Declarables proposalCompletedUserTopology() {
        return consumerTopology("proposal.events", "proposal.completed", "proposal.completed.user");
    }

    @Bean
    public Declarables proposalCancelledUserTopology() {
        return consumerTopology("proposal.events", "proposal.cancelled", "proposal.cancelled.user");
    }

    private Declarables consumerTopology(String exchangeName, String routingKey, String queuePrefix) {
        TopicExchange exchange = new TopicExchange(exchangeName);
        TopicExchange deadLetterExchange = new TopicExchange(queuePrefix + ".dlx");
        Queue queue = QueueBuilder.durable(queuePrefix + ".queue")
                .withArgument("x-dead-letter-exchange", queuePrefix + ".dlx")
                .withArgument("x-dead-letter-routing-key", queuePrefix + ".dlq")
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(queuePrefix + ".dlq").build();
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(queuePrefix + ".dlq");

        return new Declarables(exchange, queue, deadLetterQueue, deadLetterExchange, binding, deadLetterBinding);
    }
}
