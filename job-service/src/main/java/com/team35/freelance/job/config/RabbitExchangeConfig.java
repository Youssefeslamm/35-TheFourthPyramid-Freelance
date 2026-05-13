package com.team35.freelance.job.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitExchangeConfig {

    @Bean
    public TopicExchange jobEventsExchange() {
        return new TopicExchange("job.events");
    }
}
