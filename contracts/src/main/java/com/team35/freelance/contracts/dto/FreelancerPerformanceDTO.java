package com.team35.freelance.contracts.dto;

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

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Integer getTotalContracts() {
        return totalContracts;
    }

    public void setTotalContracts(Integer totalContracts) {
        this.totalContracts = totalContracts;
    }

    public Double getAverageContractValue() {
        return averageContractValue;
    }

    public void setAverageContractValue(Double averageContractValue) {
        this.averageContractValue = averageContractValue;
    }

    public Double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(Double completionRate) {
        this.completionRate = completionRate;
    }

    public Double getAverageDurationDays() {
        return averageDurationDays;
    }

    public void setAverageDurationDays(Double averageDurationDays) {
        this.averageDurationDays = averageDurationDays;
    }

    public Double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(Double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }
}
