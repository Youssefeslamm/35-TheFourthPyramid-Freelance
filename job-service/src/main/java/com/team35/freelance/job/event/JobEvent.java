package com.team35.freelance.job.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "job_events")
public class JobEvent implements MongoEvent {

    @Id
    private String id;

    private Long jobId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    public JobEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public JobEvent(Long jobId, String action, Map<String, Object> details) {
        this.jobId = jobId;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
