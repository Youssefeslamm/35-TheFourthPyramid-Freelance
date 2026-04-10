package com.team35.freelance.proposal.dto;

import java.util.HashMap;
import java.util.Map;

public class MilestoneRequest {

    private String title;
    private String description;
    private Double amount;
    private Map<String, Object> metadata = new HashMap<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}