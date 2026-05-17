package com.team35.freelance.contract.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class MilestoneTrackRequestDTO {
    private Integer milestoneOrder;
    private String status;
    @JsonAlias("recorded_by")
    private Long recordedBy;
    private String notes;

    public MilestoneTrackRequestDTO() {}

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
