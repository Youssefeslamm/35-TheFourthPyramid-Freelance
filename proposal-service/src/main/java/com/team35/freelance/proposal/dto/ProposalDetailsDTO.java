package com.team35.freelance.proposal.dto;

import com.team35.freelance.proposal.model.ProposalStatus;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ProposalDetailsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long proposalId;
    private Long jobId;
    private Long freelancerId;
    private ProposalStatus status;
    private Double bidAmount;
    private Map<String, Object> metadata;
    private List<MilestoneDTO> milestones;
    private int totalMilestones;
    private long completedMilestones;

    private ProposalDetailsDTO(Builder builder) {
        this.proposalId = builder.proposalId;
        this.jobId = builder.jobId;
        this.freelancerId = builder.freelancerId;
        this.status = builder.status;
        this.bidAmount = builder.bidAmount;
        this.metadata = builder.metadata;
        this.milestones = builder.milestones;
        this.totalMilestones = builder.totalMilestones;
        this.completedMilestones = builder.completedMilestones;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long proposalId;
        private Long jobId;
        private Long freelancerId;
        private ProposalStatus status;
        private Double bidAmount;
        private Map<String, Object> metadata;
        private List<MilestoneDTO> milestones;
        private int totalMilestones;
        private long completedMilestones;

        public Builder proposalId(Long proposalId) {
            this.proposalId = proposalId;
            return this;
        }

        public Builder jobId(Long jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder freelancerId(Long freelancerId) {
            this.freelancerId = freelancerId;
            return this;
        }

        public Builder status(ProposalStatus status) {
            this.status = status;
            return this;
        }

        public Builder bidAmount(Double bidAmount) {
            this.bidAmount = bidAmount;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder milestones(List<MilestoneDTO> milestones) {
            this.milestones = milestones;
            return this;
        }

        public Builder totalMilestones(int totalMilestones) {
            this.totalMilestones = totalMilestones;
            return this;
        }

        public Builder completedMilestones(long completedMilestones) {
            this.completedMilestones = completedMilestones;
            return this;
        }

        public ProposalDetailsDTO build() {
            return new ProposalDetailsDTO(this);
        }
    }

    public Long getProposalId() {
        return proposalId;
    }

    public Long getJobId() {
        return jobId;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public List<MilestoneDTO> getMilestones() {
        return milestones;
    }

    public int getTotalMilestones() {
        return totalMilestones;
    }

    public long getCompletedMilestones() {
        return completedMilestones;
    }
}