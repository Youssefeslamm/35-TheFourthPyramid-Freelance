package com.team35.freelance.proposal.dto;

public class ProposalAnalyticsDTO {

    private Long totalProposals;
    private Long acceptedProposals;
    private Long rejectedProposals;
    private Double totalBidValue;
    private Double averageBid;
    private Double acceptanceRate;

    private ProposalAnalyticsDTO(Builder builder) {
        this.totalProposals = builder.totalProposals;
        this.acceptedProposals = builder.acceptedProposals;
        this.rejectedProposals = builder.rejectedProposals;
        this.totalBidValue = builder.totalBidValue;
        this.averageBid = builder.averageBid;
        this.acceptanceRate = builder.acceptanceRate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long totalProposals;
        private Long acceptedProposals;
        private Long rejectedProposals;
        private Double totalBidValue;
        private Double averageBid;
        private Double acceptanceRate;

        public Builder totalProposals(Long totalProposals) {
            this.totalProposals = totalProposals;
            return this;
        }

        public Builder acceptedProposals(Long acceptedProposals) {
            this.acceptedProposals = acceptedProposals;
            return this;
        }

        public Builder rejectedProposals(Long rejectedProposals) {
            this.rejectedProposals = rejectedProposals;
            return this;
        }

        public Builder totalBidValue(Double totalBidValue) {
            this.totalBidValue = totalBidValue;
            return this;
        }

        public Builder averageBid(Double averageBid) {
            this.averageBid = averageBid;
            return this;
        }

        public Builder acceptanceRate(Double acceptanceRate) {
            this.acceptanceRate = acceptanceRate;
            return this;
        }

        public ProposalAnalyticsDTO build() {
            return new ProposalAnalyticsDTO(this);
        }
    }

    public Long getTotalProposals() {
        return totalProposals;
    }

    public Long getAcceptedProposals() {
        return acceptedProposals;
    }

    public Long getRejectedProposals() {
        return rejectedProposals;
    }

    public Double getTotalBidValue() {
        return totalBidValue;
    }

    public Double getAverageBid() {
        return averageBid;
    }

    public Double getAcceptanceRate() {
        return acceptanceRate;
    }
}