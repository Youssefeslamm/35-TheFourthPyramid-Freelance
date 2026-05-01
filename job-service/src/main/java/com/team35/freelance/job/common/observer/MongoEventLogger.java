package com.team35.freelance.job.common.observer;

import com.team35.freelance.job.common.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private final MongoTemplate mongoTemplate;
    private final EventFactory eventFactory;

    private static final Logger log =
            LoggerFactory.getLogger(MongoEventLogger.class);

    public MongoEventLogger(MongoTemplate mongoTemplate,
                            EventFactory eventFactory) {
        this.mongoTemplate = mongoTemplate;
        this.eventFactory = eventFactory;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            Map<String, Object> params = (Map<String, Object>) payload;

            MongoEvent event = eventFactory.createEvent(EventType.JOB, params);

            mongoTemplate.save(event, "job_events");

        } catch (Exception e) {
            log.warn("Mongo logging failed: {}", e.getMessage());
        }
    }
}