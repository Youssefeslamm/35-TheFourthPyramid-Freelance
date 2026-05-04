package com.team35.freelance.job.service;

import com.team35.freelance.job.model.MongoEvent;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component("jobServiceEventFactory")
public class EventFactory {

    /**
     * Creates and returns a MongoEvent.
     * This method is called by JobService to log audit events.
     */
    public MongoEvent createEvent(Long jobId, String action, Object payload) {
        MongoEvent event = new MongoEvent();
        event.setJobId(jobId);
        event.setAction(action);
        event.setPayload(payload);
        event.setTimestamp(LocalDateTime.now());
        // Note: You can add logic here to save to a repository
        // if you want the factory to handle persistence.
        return event;
    }
}