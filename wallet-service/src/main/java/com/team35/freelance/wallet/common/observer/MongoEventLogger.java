package com.team35.freelance.wallet.common.observer;

import com.team35.freelance.wallet.common.event.*;
import com.team35.freelance.wallet.repository.MongoEventRepository;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component
public class MongoEventLogger implements EntityObserver {

    private final EventFactory eventFactory;
    private final MongoEventRepository mongoEventRepository;

    private static final Logger log =
            LoggerFactory.getLogger(MongoEventLogger.class);

    public MongoEventLogger(EventFactory eventFactory,
                            MongoEventRepository mongoEventRepository) {
        this.eventFactory = eventFactory;
        this.mongoEventRepository = mongoEventRepository;
    }

    @Override
    public void onEvent(String eventType, Object payload) {

        try {
            EventType type = EventType.valueOf(eventType);

            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) payload;

            MongoEvent event = eventFactory.createEvent(type, params);

            mongoEventRepository.save((PayoutAuditEvent) event);

            log.info("Mongo Event Saved: {}", event.getAction());

        } catch (Exception e) {
            log.warn("Mongo logging failed: {}", e.getMessage());
        }
    }
}