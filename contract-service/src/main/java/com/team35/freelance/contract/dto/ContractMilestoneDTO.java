package com.team35.freelance.contract.dto;

import java.time.Instant;

public class ContractMilestoneDTO {

    private Instant timestamp;
    private Integer milestoneOrder;
    private String status;
    private Long recordedBy;
    private String notes;

    public ContractMilestoneDTO() {
    }

    public ContractMilestoneDTO(Instant timestamp, Integer milestoneOrder, String status, Long recordedBy, String notes) {
        this.timestamp = timestamp;
        this.milestoneOrder = milestoneOrder;
        this.status = status;
        this.recordedBy = recordedBy;
        this.notes = notes;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getMilestoneOrder() {
        return milestoneOrder;
    }

    public void setMilestoneOrder(Integer milestoneOrder) {
        this.milestoneOrder = milestoneOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
