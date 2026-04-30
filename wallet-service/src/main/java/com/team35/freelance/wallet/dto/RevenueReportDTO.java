package com.team35.freelance.wallet.dto;

import java.io.Serializable;

public class RevenueReportDTO implements Serializable {

    private double totalRevenue;
    private long totalTransactions;
    private double averagePayout;
    private double refundedAmount;
    private long refundCount;

    public RevenueReportDTO(double totalRevenue,
                            long totalTransactions,
                            double averagePayout,
                            double refundedAmount,
                            long refundCount) {
        this.totalRevenue = totalRevenue;
        this.totalTransactions = totalTransactions;
        this.averagePayout = averagePayout;
        this.refundedAmount = refundedAmount;
        this.refundCount = refundCount;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public double getAveragePayout() {
        return averagePayout;
    }

    public double getRefundedAmount() {
        return refundedAmount;
    }

    public long getRefundCount() {
        return refundCount;
    }
}