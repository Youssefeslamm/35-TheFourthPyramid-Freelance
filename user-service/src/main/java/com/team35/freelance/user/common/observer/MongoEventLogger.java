package com.team35.freelance.user.common.observer;

import com.team35.freelance.user.common.event.EventFactory;
import com.team35.freelance.user.common.event.EventType;
import com.team35.freelance.user.common.event.MongoEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private final EventFactory eventFactory;
    private final MongoTemplate mongoTemplate;   // ✅ FIX 1

    private static final Logger log =
            LoggerFactory.getLogger(MongoEventLogger.class);

    // ✅ FIX 2: inject BOTH dependencies
    public MongoEventLogger(EventFactory eventFactory,
                            MongoTemplate mongoTemplate) {
        this.eventFactory = eventFactory;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void onEvent(String eventType, Object payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) payload;

            params.put("action", eventType);

            MongoEvent event = eventFactory.createEvent(EventType.AUTH, params);

            mongoTemplate.save(event);

        } catch (Exception e) {
            log.warn("Mongo logging failed: {}", e.getMessage());
        }
    }
}