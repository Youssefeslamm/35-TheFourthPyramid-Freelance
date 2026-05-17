package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.FreelancerPayoutSummaryDTO;
import com.team35.freelance.contracts.dto.PayoutDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@FeignClient(name = "wallet-service", url = "${feign.wallet-service.url}")
public interface WalletServiceClient {

    @GetMapping("/api/payouts/{id}")
    PayoutDTO getPayoutById(@PathVariable("id") Long id);

    @GetMapping("/api/payouts/freelancer/{freelancerId}/summary")
    FreelancerPayoutSummaryDTO getFreelancerSummary(@PathVariable("freelancerId") Long freelancerId);

    @GetMapping("/api/payouts/freelancer/{freelancerId}/total")
    BigDecimal getFreelancerPayoutTotal(
            @PathVariable("freelancerId") Long freelancerId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);
}
