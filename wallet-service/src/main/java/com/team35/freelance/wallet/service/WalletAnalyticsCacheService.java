package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.dto.CategoryRevenueDTO;
import com.team35.freelance.wallet.repository.PayoutRepository;
import org.springframework.cache.annotation.Cacheable;
import com.team35.freelance.wallet.service.PayoutService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WalletAnalyticsCacheService {

    private final PayoutRepository payoutRepository;

    public WalletAnalyticsCacheService(PayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

        @Cacheable(
                value = "wallet-service::S5-F10",
                key = "#startDate.toString() + ':' + #endDate.toString()"
        )
        public List<CategoryRevenueDTO> getCategoryRevenueAnalyticsCached (LocalDate startDate, LocalDate endDate){

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999_000_000);

            List<Object[]> rows = payoutRepository.getCategoryRevenueAnalytics(startDateTime, endDateTime);

            return rows.stream()
                    .map(row -> {
                        String category = String.valueOf(row[0]);
                        double platformFeeRevenue = ((Number) row[1]).doubleValue();
                        double totalRevenue = ((Number) row[2]).doubleValue();
                        long payoutCount = ((Number) row[3]).longValue();

                        return CategoryRevenueDTO.builder()
                                .category(category)
                                .platformFeeRevenue(platformFeeRevenue)
                                .totalRevenue(totalRevenue)
                                .netPayoutRevenue(totalRevenue - platformFeeRevenue)
                                .payoutCount(payoutCount)
                                .build();
                    })
                    .toList();
        }
    }

