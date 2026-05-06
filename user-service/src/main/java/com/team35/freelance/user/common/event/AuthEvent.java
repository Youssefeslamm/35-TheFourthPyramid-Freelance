package com.team35.freelance.user.common.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Map;

@Document(collection = "auth_events")
public class AuthEvent implements MongoEvent {

    @Id
    private String id;

    private Long userId;
    private String timestamp;
    private String action;
    private Map<String, Object> details;

    public AuthEvent(Map<String, Object> params) {
        this.id = ObjectId.get().toHexString();
        this.timestamp = Instant.now().toString();
        this.action = (String) params.get("action");
        this.details = params;
        Object uid = params.get("userId");
        if (uid instanceof Long l) {
            this.userId = l;
        } else if (uid instanceof Integer i) {
            this.userId = i.longValue();
        } else if (uid instanceof Number n) {
            this.userId = n.longValue();
        }
    }

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public Map<String, Object> getDetails() { return details; }
}
