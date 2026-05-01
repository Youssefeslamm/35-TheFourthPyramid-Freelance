package com.team35.freelance.proposal.common.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "proposal_events")
public class ProposalEvent implements MongoEvent {

    @Id
    private String id;

    private Instant timestamp;
    private String action;
    private Map<String, Object> details;

    public ProposalEvent(String action, Map<String, Object> details) {
        this.timestamp = Instant.now();
        this.action = action;
        this.details = details;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Instant getTimestamp() {
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
}