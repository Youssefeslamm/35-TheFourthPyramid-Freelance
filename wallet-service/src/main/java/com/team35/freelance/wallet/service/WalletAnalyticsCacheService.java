package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.dto.CategoryRevenueDTO;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.service.PayoutService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WalletAnalyticsCacheService {

    private final PayoutRepository payoutRepository;

    public WalletAnalyticsCacheService(PayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

        @Cacheable(value = "wallet-service::S5-F10", key = "#startDate + ':' + #endDate")
        public List<CategoryRevenueDTO> getCategoryRevenueAnalyticsCached(LocalDate startDate, LocalDate endDate) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.of(23, 59, 59, 999_000_000));
            List<Object[]> rows = payoutRepository.getCategoryRevenueAnalytics(start, end);
            List<CategoryRevenueDTO> result = new ArrayList<>();

            for (Object[] row : rows) {
                double platformFeeRevenue = numberAsDouble(row, 1);
                double totalRevenue = numberAsDouble(row, 2);
                result.add(CategoryRevenueDTO.builder()
                        .category(row[0] == null ? "UNKNOWN" : String.valueOf(row[0]))
                        .platformFeeRevenue(platformFeeRevenue)
                        .netPayoutRevenue(totalRevenue - platformFeeRevenue)
                        .totalRevenue(totalRevenue)
                        .payoutCount(numberAsLong(row, 3))
                        .build());
            }

            return result;
        }

        private long numberAsLong(Object[] row, int index) {
            if (row == null || row.length <= index || row[index] == null) {
                return 0L;
            }
            return ((Number) row[index]).longValue();
        }

        private double numberAsDouble(Object[] row, int index) {
            if (row == null || row.length <= index || row[index] == null) {
                return 0.0;
            }
            return ((Number) row[index]).doubleValue();
        }
    }

