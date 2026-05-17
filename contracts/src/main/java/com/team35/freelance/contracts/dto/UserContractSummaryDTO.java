package com.team35.freelance.contracts.dto;

import java.io.Serializable;

public class UserContractSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String name;
    private Long totalContracts;
    private Long completedContracts;
    private Long terminatedContracts;
    private Double totalEarnings;
    private Double averageContractValue;

    public UserContractSummaryDTO() {
    }

    public UserContractSummaryDTO(Long userId,
                                  String name,
                                  Long totalContracts,
                                  Long completedContracts,
                                  Long terminatedContracts,
                                  Double totalEarnings,
                                  Double averageContractValue) {
        this.userId = userId;
        this.name = name;
        this.totalContracts = totalContracts;
        this.completedContracts = completedContracts;
        this.terminatedContracts = terminatedContracts;
        this.totalEarnings = totalEarnings;
        this.averageContractValue = averageContractValue;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTotalContracts() {
        return totalContracts;
    }

    public void setTotalContracts(Long totalContracts) {
        this.totalContracts = totalContracts;
    }

    public Long getCompletedContracts() {
        return completedContracts;
    }

    public void setCompletedContracts(Long completedContracts) {
        this.completedContracts = completedContracts;
    }

    public Long getTerminatedContracts() {
        return terminatedContracts;
    }

    public void setTerminatedContracts(Long terminatedContracts) {
        this.terminatedContracts = terminatedContracts;
    }

    public Double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(Double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public Double getAverageContractValue() {
        return averageContractValue;
    }

    public void setAverageContractValue(Double averageContractValue) {
        this.averageContractValue = averageContractValue;
    }
}
