package com.team35.freelance.contract.dto;

import java.util.Map;

public class ContractAnalyticsDTO {

    private Long totalContracts;
    private Double averageContractValue;
    private Double completionRate;
    private Double averageContractDurationDays;
    private Map<String, Long> contractsByStatus;

    public ContractAnalyticsDTO() {
    }

    public ContractAnalyticsDTO(Long totalContracts,
                                Double averageContractValue,
                                Double completionRate,
                                Double averageContractDurationDays,
                                Map<String, Long> contractsByStatus) {
        this.totalContracts = totalContracts;
        this.averageContractValue = averageContractValue;
        this.completionRate = completionRate;
        this.averageContractDurationDays = averageContractDurationDays;
        this.contractsByStatus = contractsByStatus;
    }

    public Long getTotalContracts() {
        return totalContracts;
    }

    public void setTotalContracts(Long totalContracts) {
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

    public Double getAverageContractDurationDays() {
        return averageContractDurationDays;
    }

    public void setAverageContractDurationDays(Double averageContractDurationDays) {
        this.averageContractDurationDays = averageContractDurationDays;
    }

    public Map<String, Long> getContractsByStatus() {
        return contractsByStatus;
    }

    public void setContractsByStatus(Map<String, Long> contractsByStatus) {
        this.contractsByStatus = contractsByStatus;
    }
}
