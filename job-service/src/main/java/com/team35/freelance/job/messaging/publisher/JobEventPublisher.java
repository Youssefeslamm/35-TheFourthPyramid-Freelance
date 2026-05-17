package com.team35.freelance.job.messaging.publisher;

import com.team35.freelance.contracts.events.JobClosedEvent;
import com.team35.freelance.contracts.events.JobRatedEvent;
import com.team35.freelance.contracts.events.JobStatusChangedEvent;
import com.team35.freelance.contracts.observability.RabbitObservability;
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
        publish("job.status-changed", event, "jobId", event.jobId());
    }

    public void publishJobRated(JobRatedEvent event) {
        publish("job.rated", event, "jobId", event.jobId());
    }

    public void publishJobClosed(JobClosedEvent event) {
        publish("job.closed", event, "jobId", event.jobId());
    }

    private void publish(String routingKey, Object payload, String entityKey, Long entityValue) {
        RabbitObservability.publish(routingKey, entityKey, entityValue,
                () -> rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload));
    }
}
