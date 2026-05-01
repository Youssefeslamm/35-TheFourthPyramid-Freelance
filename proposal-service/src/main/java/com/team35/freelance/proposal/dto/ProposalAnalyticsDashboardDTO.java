package com.team35.freelance.proposal.dto;
import java.util.Map;
public class ProposalAnalyticsDashboardDTO {
    private long totalProposals;
    private double acceptanceRate;
    private double averageBidAmount;
    private double averageEstimatedDays;
    private Map<String, Long> proposalsByStatus;
    private ProposalAnalyticsDashboardDTO() {}

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
            ProposalAnalyticsDashboardDTO dto = new ProposalAnalyticsDashboardDTO();
            dto.totalProposals = this.totalProposals;
            dto.acceptanceRate = this.acceptanceRate;
            dto.averageBidAmount = this.averageBidAmount;
            dto.averageEstimatedDays = this.averageEstimatedDays;
            dto.proposalsByStatus = this.proposalsByStatus;
            return dto;
        }
    }


    public long getTotalProposals() { return totalProposals; }
    public double getAcceptanceRate() { return acceptanceRate; }
    public double getAverageBidAmount() { return averageBidAmount; }
    public double getAverageEstimatedDays() { return averageEstimatedDays; }
    public Map<String, Long> getProposalsByStatus() { return proposalsByStatus; }
}
