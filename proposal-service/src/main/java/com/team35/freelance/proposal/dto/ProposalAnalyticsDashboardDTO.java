package com.team35.freelance.proposal.dto;
import java.util.Map;
public class ProposalAnalyticsDashboardDTO {
    private long totalProposals;
    private double acceptanceRate;
    private double averageBidAmount;
    private double averageEstimatedDays;
    private Map<String, Long> proposalsByStatus;
    private ProposalAnalyticsDashboardDTO() {}

    private ProposalAnalyticsDashboardDTO(Builder builder) {
        this.totalProposals = builder.totalProposals;
        this.acceptanceRate = builder.acceptanceRate;
        this.averageBidAmount = builder.averageBidAmount;
        this.averageEstimatedDays = builder.averageEstimatedDays;
        this.proposalsByStatus = builder.proposalsByStatus;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long totalProposals;
        private double acceptanceRate;
        private double averageBidAmount;
        private double averageEstimatedDays;
        private Map<String, Long> proposalsByStatus;

        public Builder totalProposals(long totalProposals) {
            this.totalProposals = totalProposals;
            return this;
        }
        public Builder acceptanceRate(double acceptanceRate) {
            this.acceptanceRate = acceptanceRate;
            return this;
        }
        public Builder averageBidAmount(double averageBidAmount) {
            this.averageBidAmount = averageBidAmount;
            return this;
        }
        public Builder averageEstimatedDays(double averageEstimatedDays) {
            this.averageEstimatedDays = averageEstimatedDays;
            return this;
        }
        public Builder proposalsByStatus(Map<String, Long> proposalsByStatus) {
            this.proposalsByStatus = proposalsByStatus;
            return this;
        }
        public ProposalAnalyticsDashboardDTO build() {
            return new ProposalAnalyticsDashboardDTO(this);
        }
    }


    public long getTotalProposals() { return totalProposals; }
    public double getAcceptanceRate() { return acceptanceRate; }
    public double getAverageBidAmount() { return averageBidAmount; }
    public double getAverageEstimatedDays() { return averageEstimatedDays; }
    public Map<String, Long> getProposalsByStatus() { return proposalsByStatus; }
}
