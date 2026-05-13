package com.team35.freelance.proposal.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitExchangeConfig {

    @Bean
    public TopicExchange proposalEventsExchange() {
        return new TopicExchange("proposal.events");
    }
}
