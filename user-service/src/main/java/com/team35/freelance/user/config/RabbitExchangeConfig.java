package com.team35.freelance.user.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitExchangeConfig {

    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange("user.events");
    }
}
