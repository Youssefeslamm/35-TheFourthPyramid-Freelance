package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.UserContractSummaryDTO;
import com.team35.freelance.contracts.dto.UserProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${feign.user-service.url}")
public interface UserServiceClient {
    default UserProfileDTO getUserById(Long id) {
        return getUserById(id, null, null);
    }
    @GetMapping("/api/users/{id}")
    UserProfileDTO getUserByIdInternal(@PathVariable("id") Long id,
                                       @RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId);

    @GetMapping("/api/users/{id}/contract-summary")
    UserContractSummaryDTO getUserContractSummaryInternal(@PathVariable("id") Long id,
                                                          @RequestHeader(value = "Authorization", required = false) String authorization,
                                                          @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId);

    default UserProfileDTO getUserById(Long id, String authorization) {
        return getUserById(id, authorization, null);
    }

    default UserProfileDTO getUserById(Long id, String authorization, String correlationId) {
        return FeignClientSupport.execute(
                "user-service",
                "getUserById",
                () -> getUserByIdInternal(id, authorization, correlationId),
                null
        );
    }

    default UserContractSummaryDTO getUserContractSummary(Long id, String authorization) {
        return getUserContractSummary(id, authorization, null);
    }

    default UserContractSummaryDTO getUserContractSummary(Long id, String authorization, String correlationId) {
        return FeignClientSupport.execute(
                "user-service",
                "getUserContractSummary",
                () -> getUserContractSummaryInternal(id, authorization, correlationId),
                null
        );
    }
}
