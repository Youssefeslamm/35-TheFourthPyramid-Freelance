package com.team35.freelance.wallet.common.adapter;

import com.team35.freelance.wallet.dto.RevenueReportDTO;

public class RevenueReportAdapter {

    public RevenueReportDTO adapt(Object[] row) {

        double totalRevenue = ((Number) row[0]).doubleValue();
        long totalTransactions = ((Number) row[1]).longValue();
        double refundedAmount = ((Number) row[2]).doubleValue();
        long refundCount = ((Number) row[3]).longValue();

        double averagePayout =
                totalTransactions == 0 ? 0 : totalRevenue / totalTransactions;

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .averagePayout(averagePayout)
                .refundedAmount(refundedAmount)
                .refundCount(refundCount)
                .build();
    }
}