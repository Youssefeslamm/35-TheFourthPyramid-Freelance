package com.team35.freelance.wallet.dto;

import java.io.Serializable;

public class RevenueReportDTO implements Serializable {

    private double totalRevenue;
    private long totalTransactions;
    private double averagePayout;
    private double refundedAmount;
    private long refundCount;

    private RevenueReportDTO(Builder builder) {
        this.totalRevenue = builder.totalRevenue;
        this.totalTransactions = builder.totalTransactions;
        this.averagePayout = builder.averagePayout;
        this.refundedAmount = builder.refundedAmount;
        this.refundCount = builder.refundCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double totalRevenue;
        private long totalTransactions;
        private double averagePayout;
        private double refundedAmount;
        private long refundCount;

        public Builder totalRevenue(double val) {
            this.totalRevenue = val;
            return this;
        }

        public Builder totalTransactions(long val) {
            this.totalTransactions = val;
            return this;
        }

        public Builder averagePayout(double val) {
            this.averagePayout = val;
            return this;
        }

        public Builder refundedAmount(double val) {
            this.refundedAmount = val;
            return this;
        }

        public Builder refundCount(long val) {
            this.refundCount = val;
            return this;
        }

        public RevenueReportDTO build() {
            return new RevenueReportDTO(this);
        }
    }

    public double getTotalRevenue() { return totalRevenue; }
    public long getTotalTransactions() { return totalTransactions; }
    public double getAveragePayout() { return averagePayout; }
    public double getRefundedAmount() { return refundedAmount; }
    public long getRefundCount() { return refundCount; }
}