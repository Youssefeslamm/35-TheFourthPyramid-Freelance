package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.ContractDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "contract-service", url = "${feign.contract-service.url}")
public interface ContractServiceClient {

    @GetMapping("/api/contracts/{id}")
    ContractDTO getContractByIdInternal(@PathVariable("id") Long id,
                                          @RequestHeader("X-Correlation-ID") String correlationId);

    @GetMapping("/api/contracts/user/{userId}/active")
    ContractDTO getActiveContractForUserInternal(@PathVariable("userId") Long userId,
                                                 @RequestHeader("X-Correlation-ID") String correlationId);

    default ContractDTO getContractById(Long id, String correlationId) {
        return FeignClientSupport.execute(
                "contract-service",
                "getContractById",
                () -> getContractByIdInternal(id, correlationId),
                null
        );
    }

    default ContractDTO getActiveContractForUser(Long userId, String correlationId) {
        return FeignClientSupport.execute(
                "contract-service",
                "getActiveContractForUser",
                () -> getActiveContractForUserInternal(userId, correlationId),
                null
        );
    }
}
