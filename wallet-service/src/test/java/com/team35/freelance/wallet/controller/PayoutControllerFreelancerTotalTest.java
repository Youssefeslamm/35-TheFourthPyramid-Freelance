package com.team35.freelance.wallet.controller;

import com.team35.freelance.wallet.service.PayoutPromoService;
import com.team35.freelance.wallet.service.PayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PayoutControllerFreelancerTotalTest {

    @Mock
    private PayoutService payoutService;

    @Mock
    private PayoutPromoService payoutPromoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PayoutController(payoutService, payoutPromoService)
        ).build();
    }

    @Test
    void getFreelancerPayoutTotal_returnsBigDecimalBody() throws Exception {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);

        when(payoutService.getFreelancerPayoutTotal(eq(42L), eq(start), eq(end)))
                .thenReturn(new BigDecimal("1500.50"));

        mockMvc.perform(get("/api/payouts/freelancer/42/total")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().string("1500.50"));
    }

    @Test
    void getFreelancerPayoutTotal_missingParams_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/payouts/freelancer/42/total"))
                .andExpect(status().isBadRequest());
    }
}
