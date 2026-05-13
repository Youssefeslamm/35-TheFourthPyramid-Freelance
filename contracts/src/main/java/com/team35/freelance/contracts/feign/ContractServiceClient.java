package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.ContractDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "contract-service", url = "${feign.contract-service.url}")
public interface ContractServiceClient {

    @GetMapping("/api/contracts/{id}")
    ContractDTO getContractById(@PathVariable("id") Long id);

    @GetMapping("/api/contracts/user/{userId}/active")
    ContractDTO getActiveContractForUser(@PathVariable("userId") Long userId);
}

