package com.team35.freelance.contract.dto;

import java.io.Serializable;

public class FreelancerPerformanceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long freelancerId;
    private Integer totalContracts;
    private Double averageContractValue;
    private Double completionRate;
    private Double averageDurationDays;
    private Double totalEarnings;

    public FreelancerPerformanceDTO() {
    }

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

    public void setFreelancerId(Long freelancerId) { this.freelancerId = freelancerId; }
    public void setTotalContracts(Integer totalContracts) { this.totalContracts = totalContracts; }
    public void setAverageContractValue(Double averageContractValue) { this.averageContractValue = averageContractValue; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
    public void setAverageDurationDays(Double averageDurationDays) { this.averageDurationDays = averageDurationDays; }
    public void setTotalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; }
}
