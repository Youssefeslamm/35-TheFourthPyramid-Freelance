package com.team35.freelance.user.dto;

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

    // 🔒 Private constructor
    private UserContractSummaryDTO(Builder builder) {
        this.userId = builder.userId;
        this.name = builder.name;
        this.totalContracts = builder.totalContracts;
        this.completedContracts = builder.completedContracts;
        this.terminatedContracts = builder.terminatedContracts;
        this.totalEarnings = builder.totalEarnings;
        this.averageContractValue = builder.averageContractValue;
    }

    // ✅ REQUIRED
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String name;
        private Long totalContracts;
        private Long completedContracts;
        private Long terminatedContracts;
        private Double totalEarnings;
        private Double averageContractValue;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder totalContracts(Long totalContracts) {
            this.totalContracts = totalContracts;
            return this;
        }

        public Builder completedContracts(Long completedContracts) {
            this.completedContracts = completedContracts;
            return this;
        }

        public Builder terminatedContracts(Long terminatedContracts) {
            this.terminatedContracts = terminatedContracts;
            return this;
        }

        public Builder totalEarnings(Double totalEarnings) {
            this.totalEarnings = totalEarnings;
            return this;
        }

        public Builder averageContractValue(Double averageContractValue) {
            this.averageContractValue = averageContractValue;
            return this;
        }

        public UserContractSummaryDTO build() {
            return new UserContractSummaryDTO(this);
        }
    }

    // Getters only
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public Long getTotalContracts() { return totalContracts; }
    public Long getCompletedContracts() { return completedContracts; }
    public Long getTerminatedContracts() { return terminatedContracts; }
    public Double getTotalEarnings() { return totalEarnings; }
    public Double getAverageContractValue() { return averageContractValue; }
}
