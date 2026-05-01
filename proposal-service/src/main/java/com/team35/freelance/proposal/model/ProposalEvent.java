package com.team35.freelance.proposal.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
@Document(collection = "proposal_events")
public class ProposalEvent {
    @Id
    private String id;
    private Long proposalId;
    private String action;
    private LocalDateTime timestamp;
    private Map<String, Object> details = new HashMap<>();

    public ProposalEvent() {}

    public ProposalEvent(Long proposalId, String action,
                         LocalDateTime timestamp, Map<String, Object> details) {
        this.proposalId = proposalId;
        this.action = action;
        this.timestamp = timestamp;
        this.details = details;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getProposalId() { return proposalId; }
    public void setProposalId(Long proposalId) { this.proposalId = proposalId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

}
