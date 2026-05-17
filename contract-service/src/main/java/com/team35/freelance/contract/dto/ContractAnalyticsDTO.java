package com.team35.freelance.contract.dto;

import java.io.Serializable;
import java.util.Map;

public class ContractAnalyticsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalContracts;
    private Double averageContractValue;
    private Double completionRate;
    private Double averageContractDurationDays;
    private Map<String, Long> contractsByStatus;

    public ContractAnalyticsDTO() {}

    public Long getTotalContracts() { return totalContracts; }
    public Double getAverageContractValue() { return averageContractValue; }
    public Double getCompletionRate() { return completionRate; }
    public Double getAverageContractDurationDays() { return averageContractDurationDays; }
    public Map<String, Long> getContractsByStatus() { return contractsByStatus; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ContractAnalyticsDTO dto = new ContractAnalyticsDTO();

        public Builder totalContracts(Long v) { dto.totalContracts = v; return this; }
        public Builder averageContractValue(Double v) { dto.averageContractValue = v; return this; }
        public Builder completionRate(Double v) { dto.completionRate = v; return this; }
        public Builder averageContractDurationDays(Double v) { dto.averageContractDurationDays = v; return this; }
        public Builder contractsByStatus(Map<String, Long> v) { dto.contractsByStatus = v; return this; }

        public ContractAnalyticsDTO build() { return dto; }
    }
}
