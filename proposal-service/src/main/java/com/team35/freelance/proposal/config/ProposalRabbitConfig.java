package com.team35.freelance.proposal.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
@Configuration
public class ProposalRabbitConfig {

    public static final String PROPOSAL_EXCHANGE = "proposal.events";
    public static final String CONTRACT_EXCHANGE = "contract.events";
    public static final String PAYMENT_EXCHANGE = "payment.events";

    public static final String SAGA_FEEDBACK_QUEUE = "proposal.saga-feedback";
    public static final String SAGA_FEEDBACK_DLQ = "proposal.saga-feedback.dlq";
    public static final String SAGA_FEEDBACK_DLX = "proposal.saga-feedback.dlx";

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    public TopicExchange proposalEventsExchange() {
        return new TopicExchange(PROPOSAL_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange contractEventsExchange() {
        return new TopicExchange(CONTRACT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange paymentEventsExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange proposalSagaFeedbackDlx() {
        return new TopicExchange(SAGA_FEEDBACK_DLX, true, false);
    }

    @Bean
    public Queue proposalSagaFeedbackQueue() {
        return QueueBuilder.durable(SAGA_FEEDBACK_QUEUE)
                .withArgument("x-dead-letter-exchange", SAGA_FEEDBACK_DLX)
                .withArgument("x-dead-letter-routing-key", SAGA_FEEDBACK_DLQ)
                .build();
    }

    @Bean
    public Queue proposalSagaFeedbackDlq() {
        return QueueBuilder.durable(SAGA_FEEDBACK_DLQ).build();
    }

    @Bean
    public Binding proposalSagaFeedbackDlqBinding() {
        return BindingBuilder
                .bind(proposalSagaFeedbackDlq())
                .to(proposalSagaFeedbackDlx())
                .with(SAGA_FEEDBACK_DLQ);
    }

    @Bean
    public Binding contractCreatedBinding() {
        return BindingBuilder
                .bind(proposalSagaFeedbackQueue())
                .to(contractEventsExchange())
                .with("contract.created");
    }

    @Bean
    public Binding contractStatusChangedBinding() {
        return BindingBuilder
                .bind(proposalSagaFeedbackQueue())
                .to(contractEventsExchange())
                .with("contract.status-changed");
    }

    @Bean
    public Binding contractCancelledBinding() {
        return BindingBuilder
                .bind(proposalSagaFeedbackQueue())
                .to(contractEventsExchange())
                .with("contract.cancelled");
    }

    @Bean
    public Binding paymentInitiatedBinding() {
        return BindingBuilder
                .bind(proposalSagaFeedbackQueue())
                .to(paymentEventsExchange())
                .with("payment.initiated");
    }

    @Bean
    public Binding paymentCompletedBinding() {
        return BindingBuilder
                .bind(proposalSagaFeedbackQueue())
                .to(paymentEventsExchange())
                .with("payment.completed");
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(proposalSagaFeedbackQueue())
                .to(paymentEventsExchange())
                .with("payment.failed");
    }

    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder
                .bind(proposalSagaFeedbackQueue())
                .to(paymentEventsExchange())
                .with("payment.refunded");
    }
    // Add this bean to ProposalRabbitConfig.java
    @Bean
    public org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate(
            org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {
        org.springframework.amqp.rabbit.core.RabbitTemplate template =
                new org.springframework.amqp.rabbit.core.RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }
}