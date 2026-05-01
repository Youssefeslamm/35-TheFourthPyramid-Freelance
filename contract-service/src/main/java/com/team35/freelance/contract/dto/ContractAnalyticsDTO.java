package com.team35.freelance.contract.dto;

import java.util.Map;

public class ContractAnalyticsDTO {
    private long totalContracts;
    private double averageContractValue;
    private double completionRate;
    private double averageContractDurationDays;
    private Map<String, Long> contractsByStatus;

    private ContractAnalyticsDTO() {}

    public long getTotalContracts() { return totalContracts; }
    public double getAverageContractValue() { return averageContractValue; }
    public double getCompletionRate() { return completionRate; }
    public double getAverageContractDurationDays() { return averageContractDurationDays; }
    public Map<String, Long> getContractsByStatus() { return contractsByStatus; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ContractAnalyticsDTO dto = new ContractAnalyticsDTO();

        public Builder totalContracts(long v) { dto.totalContracts = v; return this; }
        public Builder averageContractValue(double v) { dto.averageContractValue = v; return this; }
        public Builder completionRate(double v) { dto.completionRate = v; return this; }
        public Builder averageContractDurationDays(double v) { dto.averageContractDurationDays = v; return this; }
        public Builder contractsByStatus(Map<String, Long> v) { dto.contractsByStatus = v; return this; }
        public ContractAnalyticsDTO build() { return dto; }
    }
}
