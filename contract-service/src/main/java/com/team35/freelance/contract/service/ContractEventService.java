package com.team35.freelance.contract.service;

import com.team35.freelance.contract.common.event.EventFactory;
import com.team35.freelance.contract.common.event.EventType;
import com.team35.freelance.contract.common.event.MongoEvent;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContractEventService {

    private final EventFactory eventFactory;
    private final MongoTemplate mongoTemplate;

    public ContractEventService(EventFactory eventFactory, MongoTemplate mongoTemplate) {
        this.eventFactory = eventFactory;
        this.mongoTemplate = mongoTemplate;
    }

    public void logAnalyticsViewed(LocalDateTime startDate, LocalDateTime endDate) {
        logAnalyticsViewed(startDate, endDate, null);
    }

    public void logAnalyticsViewed(LocalDateTime startDate, LocalDateTime endDate, Long userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "ANALYTICS_VIEWED");
        payload.put("eventType", "ANALYTICS_VIEWED");
        payload.put("timestamp", LocalDateTime.now());
        payload.put("startDate", startDate);
        payload.put("endDate", endDate);
        if (userId != null) {
            payload.put("userId", userId);
        }
        saveEvent(payload);
    }

    public void logMilestoneTracked(Long contractId,
                                    Integer milestoneOrder,
                                    String status,
                                    String recordedBy,
                                    String notes) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "MILESTONE_TRACKED");
        payload.put("eventType", "MILESTONE_TRACKED");
        payload.put("timestamp", LocalDateTime.now());
        payload.put("contractId", contractId);
        payload.put("milestoneOrder", milestoneOrder);
        payload.put("status", status);
        payload.put("recordedBy", recordedBy);
        payload.put("notes", notes);
        saveEvent(payload);
    }

    private void saveEvent(Map<String, Object> payload) {
        MongoEvent event = eventFactory.createEvent(EventType.CONTRACT, payload);
        mongoTemplate.save(event);
    }
}
