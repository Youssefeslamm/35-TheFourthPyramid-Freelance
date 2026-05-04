package com.team35.freelance.job.common.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class JobEvent implements MongoEvent {

    private String id;
    private LocalDateTime timestamp;
    private String action;
    private Map<String, Object> details;

    public JobEvent(String action, Map<String, Object> details) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.details = details;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public Map<String, Object> getDetails() { return details; }
}