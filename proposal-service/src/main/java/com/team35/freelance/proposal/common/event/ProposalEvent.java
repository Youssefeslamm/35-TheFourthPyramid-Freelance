package com.team35.freelance.proposal.common.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "proposal_events")
public class ProposalEvent implements MongoEvent {

    @Id
    private String id;

    private Long proposalId;
    private String action;
    private Instant timestamp;
    private Map<String, Object> details;

    public ProposalEvent() {
        this.timestamp = Instant.now();
    }

    public ProposalEvent(Long proposalId, String action, Map<String, Object> details) {
        this.proposalId = proposalId;
        this.action = action;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public String getAction() {
        return action;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}