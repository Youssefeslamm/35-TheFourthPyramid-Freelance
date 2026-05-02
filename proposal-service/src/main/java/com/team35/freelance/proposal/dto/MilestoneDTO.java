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

    private MilestoneDTO(Builder builder) {
        this.id = builder.id;
        this.milestoneOrder = builder.milestoneOrder;
        this.title = builder.title;
        this.description = builder.description;
        this.amount = builder.amount;
        this.status = builder.status;
        this.metadata = builder.metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Integer milestoneOrder;
        private String title;
        private String description;
        private Double amount;
        private MilestoneStatus status;
        private Map<String, Object> metadata;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder milestoneOrder(Integer milestoneOrder) {
            this.milestoneOrder = milestoneOrder;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public Builder status(MilestoneStatus status) {
            this.status = status;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public MilestoneDTO build() {
            return new MilestoneDTO(this);
        }
    }
    public Long getId() { return id; }
    public Integer getMilestoneOrder() { return milestoneOrder; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Double getAmount() { return amount; }
    public MilestoneStatus getStatus() { return status; }
    public Map<String, Object> getMetadata() { return metadata; }
}
