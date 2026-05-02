package com.team35.freelance.contract.dto;

public class FreelancerPerformanceDTO {
    private Long freelancerId;
    private Integer totalContracts;
    private Double averageContractValue;
    private Double completionRate;
    private Double averageDurationDays;
    private Double totalEarnings;

    private FreelancerPerformanceDTO(Builder builder) {
        this.freelancerId = builder.freelancerId;
        this.totalContracts = builder.totalContracts;
        this.averageContractValue = builder.averageContractValue;
        this.completionRate = builder.completionRate;
        this.averageDurationDays = builder.averageDurationDays;
        this.totalEarnings = builder.totalEarnings;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long freelancerId;
        private Integer totalContracts;
        private Double averageContractValue;
        private Double completionRate;
        private Double averageDurationDays;
        private Double totalEarnings;

        public Builder freelancerId(Long freelancerId) {
            this.freelancerId = freelancerId;
            return this;
        }

        public Builder totalContracts(Integer totalContracts) {
            this.totalContracts = totalContracts;
            return this;
        }

        public Builder averageContractValue(Double averageContractValue) {
            this.averageContractValue = averageContractValue;
            return this;
        }

        public Builder completionRate(Double completionRate) {
            this.completionRate = completionRate;
            return this;
        }

        public Builder averageDurationDays(Double averageDurationDays) {
            this.averageDurationDays = averageDurationDays;
            return this;
        }

        public Builder totalEarnings(Double totalEarnings) {
            this.totalEarnings = totalEarnings;
            return this;
        }

        public FreelancerPerformanceDTO build() {
            return new FreelancerPerformanceDTO(this);
        }
    }

    // Getters
    public Long getFreelancerId() { return freelancerId; }
    public Integer getTotalContracts() { return totalContracts; }
    public Double getAverageContractValue() { return averageContractValue; }
    public Double getCompletionRate() { return completionRate; }
    public Double getAverageDurationDays() { return averageDurationDays; }
    public Double getTotalEarnings() { return totalEarnings; }
}