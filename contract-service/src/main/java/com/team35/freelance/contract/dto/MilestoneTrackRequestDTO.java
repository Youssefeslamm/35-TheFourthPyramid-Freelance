package com.team35.freelance.contract.dto;

public class MilestoneTrackRequestDTO {
    private Integer milestoneOrder;
    private String status;
    private String recordedBy;
    private String notes;

    public MilestoneTrackRequestDTO() {}

    public Integer getMilestoneOrder() { return milestoneOrder; }
    public void setMilestoneOrder(Integer milestoneOrder) { this.milestoneOrder = milestoneOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
