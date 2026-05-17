package com.team35.freelance.contract.dto;

import java.io.Serializable;

public class StalledContractDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long contractId;
    private String freelancerName;
    private String jobTitle;
    private Double agreedAmount;
    private Double progressPercentage;
    private Integer daysSinceLastActivity;

    private StalledContractDTO(Builder builder) {
        this.contractId = builder.contractId;
        this.freelancerName = builder.freelancerName;
        this.jobTitle = builder.jobTitle;
        this.agreedAmount = builder.agreedAmount;
        this.progressPercentage = builder.progressPercentage;
        this.daysSinceLastActivity = builder.daysSinceLastActivity;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long contractId;
        private String freelancerName;
        private String jobTitle;
        private Double agreedAmount;
        private Double progressPercentage;
        private Integer daysSinceLastActivity;

        public Builder contractId(Long contractId) {
            this.contractId = contractId;
            return this;
        }

        public Builder freelancerName(String freelancerName) {
            this.freelancerName = freelancerName;
            return this;
        }

        public Builder jobTitle(String jobTitle) {
            this.jobTitle = jobTitle;
            return this;
        }

        public Builder agreedAmount(Double agreedAmount) {
            this.agreedAmount = agreedAmount;
            return this;
        }

        public Builder progressPercentage(Double progressPercentage) {
            this.progressPercentage = progressPercentage;
            return this;
        }

        public Builder daysSinceLastActivity(Integer daysSinceLastActivity) {
            this.daysSinceLastActivity = daysSinceLastActivity;
            return this;
        }

        public StalledContractDTO build() {
            return new StalledContractDTO(this);
        }
    }

    // Getters
    public Long getContractId() { return contractId; }
    public String getFreelancerName() { return freelancerName; }
    public String getJobTitle() { return jobTitle; }
    public Double getAgreedAmount() { return agreedAmount; }
    public Double getProgressPercentage() { return progressPercentage; }
    public Integer getDaysSinceLastActivity() { return daysSinceLastActivity; }
}
