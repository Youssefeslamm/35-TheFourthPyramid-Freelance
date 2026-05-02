package com.team35.freelance.contract.dto;

public class ContractSummaryDTO {

    private Long contractId;

    private String freelancerName;
    private String jobTitle;
    private Double agreedAmount;
    private String status;
    private Double durationDays;

    private ContractSummaryDTO(Builder builder) {
        this.contractId = builder.contractId;
        this.freelancerName = builder.freelancerName;
        this.jobTitle = builder.jobTitle;
        this.agreedAmount = builder.agreedAmount;
        this.status = builder.status;
        this.durationDays = builder.durationDays;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long contractId;
        private String freelancerName;
        private String jobTitle;
        private Double agreedAmount;
        private String status;
        private Double durationDays;

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

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder durationDays(Double durationDays) {
            this.durationDays = durationDays;
            return this;
        }

        public ContractSummaryDTO build() {
            return new ContractSummaryDTO(this);
        }
    }

    public Long getContractId() {
        return contractId;
    }

    public String getFreelancerName() {
        return freelancerName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public Double getAgreedAmount() {
        return agreedAmount;
    }

    public String getStatus() {
        return status;
    }

    public Double getDurationDays() {
        return durationDays;
    }
}
