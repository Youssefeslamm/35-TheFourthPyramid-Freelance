package com.team35.freelance.job.common.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "job_events")
public class JobEvent implements MongoEvent {

    @Id
    private String id;
    private Long jobId;
    private LocalDateTime timestamp;
    private String action;
    private Map<String, Object> details;

    public JobEvent() {
    }

    public JobEvent(String action, Map<String, Object> details) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.details = details;

        Object jobIdValue = details == null ? null : details.get("jobId");
        if (jobIdValue instanceof Number number) {
            this.jobId = number.longValue();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setId(String id) { this.id = id; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setAction(String action) { this.action = action; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
