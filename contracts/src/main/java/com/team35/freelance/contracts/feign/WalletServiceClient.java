package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.FreelancerPayoutSummaryDTO;
import com.team35.freelance.contracts.dto.PayoutDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "wallet-service", url = "${feign.wallet-service.url}")
public interface WalletServiceClient {

    @GetMapping("/api/payouts/{id}")
    PayoutDTO getPayoutByIdInternal(@PathVariable("id") Long id,
                                    @RequestHeader("X-Correlation-ID") String correlationId);

    @GetMapping("/api/payouts/freelancer/{freelancerId}/summary")
    FreelancerPayoutSummaryDTO getFreelancerSummaryInternal(@PathVariable("freelancerId") Long freelancerId,
                                                            @RequestHeader("X-Correlation-ID") String correlationId);

    @GetMapping("/api/payouts/freelancer/{freelancerId}/total")
    BigDecimal getFreelancerPayoutTotalInternal(@PathVariable("freelancerId") Long freelancerId,
                                                @RequestParam("startDate") String startDate,
                                                @RequestParam("endDate") String endDate,
                                                @RequestHeader("X-Correlation-ID") String correlationId);

    default PayoutDTO getPayoutById(Long id, String correlationId) {
        return FeignClientSupport.execute(
                "wallet-service",
                "getPayoutById",
                () -> getPayoutByIdInternal(id, correlationId),
                null
        );
    }

    default FreelancerPayoutSummaryDTO getFreelancerSummary(Long freelancerId, String correlationId) {
        return FeignClientSupport.execute(
                "wallet-service",
                "getFreelancerSummary",
                () -> getFreelancerSummaryInternal(freelancerId, correlationId),
                null
        );
    }

    default BigDecimal getFreelancerPayoutTotal(Long freelancerId, String startDate, String endDate, String correlationId) {
        return FeignClientSupport.execute(
                "wallet-service",
                "getFreelancerPayoutTotal",
                () -> getFreelancerPayoutTotalInternal(freelancerId, startDate, endDate, correlationId),
                BigDecimal.ZERO
        );
    }
}
