package com.team35.freelance.user.common.adapter;

import com.team35.freelance.user.dto.UserContractSummaryDTO;

public class UserContractSummaryAdapter {

    public UserContractSummaryDTO adapt(Object[] row) {
        if (row == null) {
            return UserContractSummaryDTO.builder()
                    .userId(0L)
                    .name(null)
                    .totalContracts(0L)
                    .completedContracts(0L)
                    .terminatedContracts(0L)
                    .totalEarnings(0.0)
                    .averageContractValue(0.0)
                    .build();
        }

        return UserContractSummaryDTO.builder()
                .userId(numberAsLong(row, 0))
                .name(row.length > 1 && row[1] != null ? (String) row[1] : null)
                .totalContracts(numberAsLong(row, 2))
                .completedContracts(numberAsLong(row, 3))
                .terminatedContracts(numberAsLong(row, 4))
                .totalEarnings(numberAsDouble(row, 5))
                .averageContractValue(numberAsDouble(row, 6))
                .build();
    }

    private Long numberAsLong(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return 0L;
        }
        return ((Number) row[index]).longValue();
    }

    private Double numberAsDouble(Object[] row, int index) {
        if (index >= row.length || row[index] == null) {
            return 0.0;
        }
        return ((Number) row[index]).doubleValue();
    }
}
