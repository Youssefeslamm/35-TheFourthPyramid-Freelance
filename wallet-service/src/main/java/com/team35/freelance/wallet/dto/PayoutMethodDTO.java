package com.team35.freelance.wallet.dto;

import java.io.Serializable;

public class PayoutMethodDTO implements Serializable {

    private String method;
    private long successCount;
    private long failureCount;
    private double successRate;
    private double totalAmount;

    private PayoutMethodDTO() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final PayoutMethodDTO dto = new PayoutMethodDTO();

        public Builder method(String method)       { dto.method = method; return this; }
        public Builder successCount(long v)        { dto.successCount = v; return this; }
        public Builder failureCount(long v)        { dto.failureCount = v; return this; }
        public Builder successRate(double v)       { dto.successRate = v; return this; }
        public Builder totalAmount(double v)       { dto.totalAmount = v; return this; }

        public PayoutMethodDTO build() { return dto; }
    }

    public String getMethod()      { return method; }
    public long getSuccessCount()  { return successCount; }
    public long getFailureCount()  { return failureCount; }
    public double getSuccessRate() { return successRate; }
    public double getTotalAmount() { return totalAmount; }
}