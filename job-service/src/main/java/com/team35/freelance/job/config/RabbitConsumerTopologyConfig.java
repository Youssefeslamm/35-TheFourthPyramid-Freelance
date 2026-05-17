package com.team35.freelance.job.config;

import java.util.HashMap;
import java.util.Map;

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

    public static final String JOB_PROPOSAL_SAGA_QUEUE = "job.proposal.saga-listener";
    public static final String JOB_PROPOSAL_SAGA_DLQ = "job.proposal.saga-listener.dlq";
    public static final String JOB_PROPOSAL_SAGA_DLX = "job.proposal.saga-listener.dlx";

    public static final String PROPOSAL_ACCEPTED_ROUTING_KEY = "proposal.accepted";
    public static final String PROPOSAL_COMPLETED_ROUTING_KEY = "proposal.completed";
    public static final String PROPOSAL_CANCELLED_ROUTING_KEY = "proposal.cancelled";
    public static final String PROPOSAL_WITHDRAWN_ROUTING_KEY = "proposal.withdrawn";

    @Bean
    public Declarables jobProposalSagaTopology() {
        TopicExchange proposalEventsExchange = new TopicExchange(PROPOSAL_EVENTS_EXCHANGE);
        TopicExchange deadLetterExchange = new TopicExchange(JOB_PROPOSAL_SAGA_DLX);

        Map<String, Object> queueArguments = new HashMap<>();
        queueArguments.put("x-dead-letter-exchange", JOB_PROPOSAL_SAGA_DLX);
        queueArguments.put("x-dead-letter-routing-key", JOB_PROPOSAL_SAGA_DLQ);

        Queue sagaQueue = QueueBuilder
                .durable(JOB_PROPOSAL_SAGA_QUEUE)
                .withArguments(queueArguments)
                .build();

        Queue deadLetterQueue = QueueBuilder
                .durable(JOB_PROPOSAL_SAGA_DLQ)
                .build();

        Binding acceptedBinding = BindingBuilder
                .bind(sagaQueue)
                .to(proposalEventsExchange)
                .with(PROPOSAL_ACCEPTED_ROUTING_KEY);

        Binding completedBinding = BindingBuilder
                .bind(sagaQueue)
                .to(proposalEventsExchange)
                .with(PROPOSAL_COMPLETED_ROUTING_KEY);

        Binding cancelledBinding = BindingBuilder
                .bind(sagaQueue)
                .to(proposalEventsExchange)
                .with(PROPOSAL_CANCELLED_ROUTING_KEY);

        Binding withdrawnBinding = BindingBuilder
                .bind(sagaQueue)
                .to(proposalEventsExchange)
                .with(PROPOSAL_WITHDRAWN_ROUTING_KEY);

        Binding deadLetterBinding = BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(JOB_PROPOSAL_SAGA_DLQ);

        return new Declarables(
                proposalEventsExchange,
                deadLetterExchange,
                sagaQueue,
                deadLetterQueue,
                acceptedBinding,
                completedBinding,
                cancelledBinding,
                withdrawnBinding,
                deadLetterBinding
        );
    }
}