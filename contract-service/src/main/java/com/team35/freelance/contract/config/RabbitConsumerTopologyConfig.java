package com.team35.freelance.contract.config;

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
    public Declarables contractSagaListenerTopology() {
        TopicExchange deadLetterExchange = new TopicExchange("contract.saga-listener.dlx");
        Queue queue = QueueBuilder.durable("contract.saga-listener")
                .withArgument("x-dead-letter-exchange", "contract.saga-listener.dlx")
                .withArgument("x-dead-letter-routing-key", "contract.saga-listener.dlq")
                .build();
        Queue deadLetterQueue = QueueBuilder.durable("contract.saga-listener.dlq").build();
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("contract.saga-listener.dlq");

        return new Declarables(queue, deadLetterQueue, deadLetterExchange, deadLetterBinding);
    }

    @Bean
    public Declarables userDeactivatedContractTopology() {
        return consumerTopology("user.events", "user.deactivated", "user.deactivated.contract");
    }

    @Bean
    public Declarables jobStatusChangedContractTopology() {
        return consumerTopology("job.events", "job.status-changed", "job.status-changed.contract");
    }

    @Bean
    public Declarables proposalAcceptedContractTopology() {
        return consumerTopology("proposal.events", "proposal.accepted", "proposal.accepted.contract");
    }

    @Bean
    public Declarables proposalCompletedContractTopology() {
        return consumerTopology("proposal.events", "proposal.completed", "proposal.completed.contract");
    }

    @Bean
    public Declarables proposalCancelledContractTopology() {
        return consumerTopology("proposal.events", "proposal.cancelled", "proposal.cancelled.contract");
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
