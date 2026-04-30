package com.team35.freelance.proposal.dto;
import com.team35.freelance.proposal.model.MilestoneStatus;

import java.io.Serializable;
import java.util.Map;
public class MilestoneDTO implements Serializable {
    private Long id;
    private Integer milestoneOrder;
    private String title;
    private String description;
    private Double amount;
    private MilestoneStatus status;
    private Map<String, Object> metadata;

    public MilestoneDTO(Long id, Integer milestoneOrder, String title,
                        String description, Double amount,
                        MilestoneStatus status, Map<String, Object> metadata) {
        this.id = id;
        this.milestoneOrder = milestoneOrder;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.status = status;
        this.metadata = metadata;
    }
    public Long getId() { return id; }
    public Integer getMilestoneOrder() { return milestoneOrder; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Double getAmount() { return amount; }
    public MilestoneStatus getStatus() { return status; }
    public Map<String, Object> getMetadata() { return metadata; }
}
