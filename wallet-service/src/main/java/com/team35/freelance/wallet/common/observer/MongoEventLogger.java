package com.team35.freelance.wallet.common.observer;

import com.team35.freelance.wallet.common.event.*;
import com.team35.freelance.wallet.repository.MongoEventRepository;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class MongoEventLogger implements EntityObserver {

    private final EventFactory eventFactory;
    private final MongoEventRepository mongoEventRepository;

    private static final Logger log =
            LoggerFactory.getLogger(MongoEventLogger.class);
    private static final ExecutorService AUDIT_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "wallet-audit-logger");
        thread.setDaemon(true);
        return thread;
    });

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

            CompletableFuture<Void> save = CompletableFuture.runAsync(
                    () -> mongoEventRepository.save((PayoutAuditEvent) event),
                    AUDIT_EXECUTOR
            );
            try {
                save.get(750, TimeUnit.MILLISECONDS);
                log.info("Mongo Event Saved: {}", event.getAction());
            } catch (TimeoutException e) {
                log.warn("Mongo logging timed out for action: {}", event.getAction());
            }

        } catch (Exception e) {
            log.warn("Mongo logging failed: {}", e.getMessage());
        }
    }
}
