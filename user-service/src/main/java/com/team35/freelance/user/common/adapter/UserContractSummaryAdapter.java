package com.team35.freelance.user.common.adapter;

import com.team35.freelance.user.dto.UserContractSummaryDTO;

public class UserContractSummaryAdapter {

    public UserContractSummaryDTO adapt(Object[] row) {
        return UserContractSummaryDTO.builder()
                .userId(((Number) row[0]).longValue())
                .name((String) row[1])
                .totalContracts(((Number) row[2]).longValue())
                .completedContracts(((Number) row[3]).longValue())
                .terminatedContracts(((Number) row[4]).longValue())
                .totalEarnings(((Number) row[5]).doubleValue())
                .averageContractValue(((Number) row[6]).doubleValue())
                .build();
    }
}