package com.team35.freelance.proposal.config;

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
    public Declarables userRegisteredProposalTopology() {
        return consumerTopology("user.events", "user.registered", "user.registered.proposal");
    }

    @Bean
    public Declarables userDeactivatedProposalTopology() {
        return consumerTopology("user.events", "user.deactivated", "user.deactivated.proposal");
    }

    @Bean
    public Declarables jobStatusChangedProposalTopology() {
        return consumerTopology("job.events", "job.status-changed", "job.status-changed.proposal");
    }

    @Bean
    public Declarables jobRatedProposalTopology() {
        return consumerTopology("job.events", "job.rated", "job.rated.proposal");
    }

    @Bean
    public Declarables jobClosedProposalTopology() {
        return consumerTopology("job.events", "job.closed", "job.closed.proposal");
    }

    @Bean
    public Declarables contractCreatedProposalTopology() {
        return consumerTopology("contract.events", "contract.created", "contract.created.proposal");
    }

    @Bean
    public Declarables contractStatusChangedProposalTopology() {
        return consumerTopology("contract.events", "contract.status-changed", "contract.status-changed.proposal");
    }

    @Bean
    public Declarables contractCancelledProposalTopology() {
        return consumerTopology("contract.events", "contract.cancelled", "contract.cancelled.proposal");
    }

    @Bean
    public Declarables paymentInitiatedProposalTopology() {
        return consumerTopology("payment.events", "payment.initiated", "payment.initiated.proposal");
    }

    @Bean
    public Declarables paymentCompletedProposalTopology() {
        return consumerTopology("payment.events", "payment.completed", "payment.completed.proposal");
    }

    @Bean
    public Declarables paymentFailedProposalTopology() {
        return consumerTopology("payment.events", "payment.failed", "payment.failed.proposal");
    }

    @Bean
    public Declarables paymentRefundedProposalTopology() {
        return consumerTopology("payment.events", "payment.refunded", "payment.refunded.proposal");
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
