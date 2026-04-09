package com.team35.freelance.contract.dto;

public class ContractSummaryDTO {
    private Long contractId;
    private String freelancerName;
    private String jobTitle;
    private Double agreedAmount;
    private String status;
    private Long durationDays;

    public ContractSummaryDTO() {}

    public ContractSummaryDTO(Long contractId, String freelancerName, String jobTitle, Double agreedAmount, String status, Long durationDays) {
        this.contractId = contractId;
        this.freelancerName = freelancerName;
        this.jobTitle = jobTitle;
        this.agreedAmount = agreedAmount;
        this.status = status;
        this.durationDays = durationDays;
    }

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getFreelancerName() { return freelancerName; }
    public void setFreelancerName(String freelancerName) { this.freelancerName = freelancerName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public Double getAgreedAmount() { return agreedAmount; }
    public void setAgreedAmount(Double agreedAmount) { this.agreedAmount = agreedAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDurationDays() { return durationDays; }
    public void setDurationDays(Long durationDays) { this.durationDays = durationDays; }
}
