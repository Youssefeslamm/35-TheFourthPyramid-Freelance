package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.UserContractSummaryDTO;
import com.team35.freelance.contracts.dto.UserProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserProfileDTO getUserByIdInternal(@PathVariable("id") Long id,
                                       @RequestHeader("X-Correlation-ID") String correlationId);

    @GetMapping("/api/users/{id}/contract-summary")
    UserContractSummaryDTO getUserContractSummaryInternal(@PathVariable("id") Long id,
                                                            @RequestHeader("X-Correlation-ID") String correlationId);

    default UserProfileDTO getUserById(Long id, String correlationId) {
        return FeignClientSupport.execute(
                "user-service",
                "getUserById",
                () -> getUserByIdInternal(id, correlationId),
                null
        );
    }

    default UserContractSummaryDTO getUserContractSummary(Long id, String correlationId) {
        return FeignClientSupport.execute(
                "user-service",
                "getUserContractSummary",
                () -> getUserContractSummaryInternal(id, correlationId),
                null
        );
    }
}
