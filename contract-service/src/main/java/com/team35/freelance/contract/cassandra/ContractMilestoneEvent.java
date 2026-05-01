package com.team35.freelance.contract.cassandra;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Table("contract_milestone_events")
public class ContractMilestoneEvent {

    @PrimaryKey
    private UUID id;

    private Long contractId;
    private Long milestoneId;
    private String eventType;
    private LocalDateTime eventTimestamp;
    private Map<String, String> metadata;

    public ContractMilestoneEvent() {
        this.id = UUID.randomUUID();
        this.eventTimestamp = LocalDateTime.now();
    }

    public ContractMilestoneEvent(Long contractId, Long milestoneId, String eventType, Map<String, String> metadata) {
        this.id = UUID.randomUUID();
        this.contractId = contractId;
        this.milestoneId = milestoneId;
        this.eventType = eventType;
        this.eventTimestamp = LocalDateTime.now();
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
    }

    public Long getContractId() {
        return contractId;
    }

    public Long getMilestoneId() {
        return milestoneId;
    }

    public String getEventType() {
        return eventType;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public void setMilestoneId(Long milestoneId) {
        this.milestoneId = milestoneId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}