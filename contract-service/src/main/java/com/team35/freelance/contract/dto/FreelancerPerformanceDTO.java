package com.team35.freelance.contract.dto;

public class FreelancerPerformanceDTO {
    private Long freelancerId;
    private Integer totalContracts;
    private Double averageContractValue;
    private Double completionRate;
    private Double averageDurationDays;
    private Double totalEarnings;

    public FreelancerPerformanceDTO(Long freelancerId, Integer totalContracts, Double averageContractValue, Double completionRate, Double averageDurationDays, Double totalEarnings) {
        this.freelancerId = freelancerId;
        this.totalContracts = totalContracts;
        this.averageContractValue = averageContractValue;
        this.completionRate = completionRate;
        this.averageDurationDays = averageDurationDays;
        this.totalEarnings = totalEarnings;
    }

    // Getters and Setters
    public Long getFreelancerId() { return freelancerId; }
    public void setFreelancerId(Long freelancerId) { this.freelancerId = freelancerId; }
    public Integer getTotalContracts() { return totalContracts; }
    public void setTotalContracts(Integer totalContracts) { this.totalContracts = totalContracts; }
    public Double getAverageContractValue() { return averageContractValue; }
    public void setAverageContractValue(Double averageContractValue) { this.averageContractValue = averageContractValue; }
    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
    public Double getAverageDurationDays() { return averageDurationDays; }
    public void setAverageDurationDays(Double averageDurationDays) { this.averageDurationDays = averageDurationDays; }
    public Double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; }
}