package com.team35.freelance.contract.dto;

public class StalledContractDTO {
    private Long contractId;
    private String freelancerName;
    private String jobTitle;
    private Double agreedAmount;
    private Double progressPercentage;
    private Integer daysSinceLastActivity;

    public StalledContractDTO(Long contractId, String freelancerName, String jobTitle, Double agreedAmount, Double progressPercentage, Integer daysSinceLastActivity) {
        this.contractId = contractId;
        this.freelancerName = freelancerName;
        this.jobTitle = jobTitle;
        this.agreedAmount = agreedAmount;
        this.progressPercentage = progressPercentage;
        this.daysSinceLastActivity = daysSinceLastActivity;
    }

    // Getters and Setters
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getFreelancerName() { return freelancerName; }
    public void setFreelancerName(String freelancerName) { this.freelancerName = freelancerName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public Double getAgreedAmount() { return agreedAmount; }
    public void setAgreedAmount(Double agreedAmount) { this.agreedAmount = agreedAmount; }
    public Double getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }
    public Integer getDaysSinceLastActivity() { return daysSinceLastActivity; }
    public void setDaysSinceLastActivity(Integer daysSinceLastActivity) { this.daysSinceLastActivity = daysSinceLastActivity; }
}