package com.team35.freelance.user.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuthEvent implements MongoEvent {

    private String id;
    private String timestamp;
    private String action;
    private Map<String, Object> details;

    public AuthEvent(Map<String, Object> params) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toString();
        this.action = (String) params.get("action");
        this.details = params;
    }

    public String getId() { return id; }
    public String getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public Map<String, Object> getDetails() { return details; }
}