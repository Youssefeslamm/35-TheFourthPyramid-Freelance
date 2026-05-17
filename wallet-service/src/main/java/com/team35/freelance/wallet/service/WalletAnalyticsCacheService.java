package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.dto.CategoryRevenueDTO;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.service.PayoutService;
import org.springframework.cache.annotation.Cacheable;
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

        @Cacheable(value = "wallet-service::S5-F10", key = "#startDate + ':' + #endDate")
        public List<CategoryRevenueDTO> getCategoryRevenueAnalyticsCached (LocalDate startDate, LocalDate endDate){
                    return List.of();
        }
    }

