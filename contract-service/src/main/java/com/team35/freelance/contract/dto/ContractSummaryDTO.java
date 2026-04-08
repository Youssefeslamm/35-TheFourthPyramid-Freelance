package com.team35.freelance.contract.dto;

public class ContractSummaryDTO {
    private Long contractId;
    private String freelancerName;
    private String jobTitle;
    private Double agreedAmount;
    private String status;
    private Integer durationDays;

    public ContractSummaryDTO(Long contractId, String freelancerName, String jobTitle, 
                              Double agreedAmount, String status, Integer durationDays) {
        this.contractId = contractId;
        this.freelancerName = freelancerName;
        this.jobTitle = jobTitle;
        this.agreedAmount = agreedAmount;
        this.status = status;
        this.durationDays = durationDays;
    }

    // Getters
    public Long getContractId() { return contractId; }
    public String getFreelancerName() { return freelancerName; }
    public String getJobTitle() { return jobTitle; }
    public Double getAgreedAmount() { return agreedAmount; }
    public String getStatus() { return status; }
    public Integer getDurationDays() { return durationDays; }
    
    // Setters
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public void setFreelancerName(String freelancerName) { this.freelancerName = freelancerName; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public void setAgreedAmount(Double agreedAmount) { this.agreedAmount = agreedAmount; }
    public void setStatus(String status) { this.status = status; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
}