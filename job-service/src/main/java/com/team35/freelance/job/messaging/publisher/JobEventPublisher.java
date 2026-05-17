package com.team35.freelance.job.messaging.publisher;

import com.team35.freelance.contracts.events.JobClosedEvent;
import com.team35.freelance.contracts.events.JobRatedEvent;
import com.team35.freelance.contracts.events.JobStatusChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobEventPublisher {

    private static final String EXCHANGE = "job.events";

    private final RabbitTemplate rabbitTemplate;

    public JobEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishStatusChanged(JobStatusChangedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "job.status-changed", event);
    }

    public void publishJobRated(JobRatedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "job.rated", event);
    }

    public void publishJobClosed(JobClosedEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, "job.closed", event);
    }
}

