package com.team35.freelance.job.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "job_events")
public class MongoEvent {
    @Id
    private String id;
    private Long jobId;
    private String action;
    private LocalDateTime timestamp;
    private Object payload;

    public MongoEvent() {
        this.timestamp = LocalDateTime.now();
    }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public void setAction(String action) { this.action = action; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setPayload(Object payload) { this.payload = payload; }

    // Manual Getters
    public String getId() { return id; }
    public Long getJobId() { return jobId; }
    public String getAction() { return action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Object getPayload() { return payload; }
}