package com.team35.freelance.wallet.common.observer;

import com.team35.freelance.wallet.common.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private static final Logger log = LoggerFactory.getLogger(MongoEventLogger.class);

    @Override
    public void onEvent(String eventType, Object payload) {

        try {
            EventType type = EventType.valueOf(eventType);

            Map<String, Object> params = (Map<String, Object>) payload;

            MongoEvent event = EventFactory.createEvent(type, params);

            // TODO: save to MongoDB repository
            log.info("Mongo Event Saved: {}", event.getAction());

        } catch (Exception e) {
            log.warn("Mongo logging failed: {}", e.getMessage());
        }
    }
}