package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.common.observer.MongoEventLogger;
import com.team35.freelance.wallet.common.refund.RefundStrategySelector;
import com.team35.freelance.wallet.messaging.publisher.PaymentEventPublisher;
import com.team35.freelance.wallet.repository.MongoEventRepository;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.repository.PromoCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutServiceFreelancerTotalTest {

    @Mock
    private PayoutRepository payoutRepository;
    @Mock
    private PromoCodeRepository promoCodeRepository;
    @Mock
    private RefundStrategySelector refundStrategySelector;
    @Mock
    private MongoEventLogger mongoEventLogger;
    @Mock
    private MongoEventRepository mongoEventRepository;
    @Mock
    private WalletAnalyticsCacheService walletAnalyticsCacheService;
    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private PayoutService payoutService;

    @BeforeEach
    void setUp() {
        payoutService = new PayoutService(
                payoutRepository,
                promoCodeRepository,
                refundStrategySelector,
                mongoEventLogger,
                mongoEventRepository,
                walletAnalyticsCacheService,
                paymentEventPublisher
        );
    }

    @Test
    void getFreelancerPayoutTotal_sumsCompletedPayoutsInRange() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);

        when(payoutRepository.sumCompletedPayoutTotalByFreelancerAndDateRange(
                eq(7L),
                eq(start.atStartOfDay()),
                eq(end.atTime(23, 59, 59))
        )).thenReturn(2500.75);

        BigDecimal total = payoutService.getFreelancerPayoutTotal(7L, start, end);

        assertThat(total).isEqualByComparingTo("2500.75");
        verify(payoutRepository).sumCompletedPayoutTotalByFreelancerAndDateRange(
                7L, start.atStartOfDay(), end.atTime(23, 59, 59)
        );
    }

    @Test
    void getFreelancerPayoutTotal_nullRepositoryResult_returnsZero() {
        when(payoutRepository.sumCompletedPayoutTotalByFreelancerAndDateRange(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(null);

        BigDecimal total = payoutService.getFreelancerPayoutTotal(
                1L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)
        );

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getFreelancerPayoutTotal_startAfterEnd_throwsBadRequest() {
        assertThatThrownBy(() -> payoutService.getFreelancerPayoutTotal(
                1L, LocalDate.of(2025, 12, 31), LocalDate.of(2025, 1, 1)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("startDate cannot be after endDate");
    }

    @Test
    void getFreelancerPayoutTotal_missingDates_throwsBadRequest() {
        assertThatThrownBy(() -> payoutService.getFreelancerPayoutTotal(1L, null, LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("startDate and endDate are required");
    }
}
