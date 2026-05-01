package com.team35.freelance.contract.common.observer;

import com.team35.freelance.contract.common.event.EventFactory;
import com.team35.freelance.contract.common.event.EventType;
import com.team35.freelance.contract.common.event.MongoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private final EventFactory eventFactory;
    private final MongoTemplate mongoTemplate;

    private static final Logger log =
            LoggerFactory.getLogger(MongoEventLogger.class);

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

            EventType type = EventType.CONTRACT;

            MongoEvent event = eventFactory.createEvent(type, params);

            mongoTemplate.save(event);

        } catch (Exception e) {
            log.warn("Mongo logging failed: {}", e.getMessage());
        }
    }
}

