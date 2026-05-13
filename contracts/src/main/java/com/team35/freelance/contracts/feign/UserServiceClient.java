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
    UserProfileDTO getUserById(@PathVariable("id") Long id,
                               @RequestHeader(value = "Authorization", required = false) String authorization);

    @GetMapping("/api/users/{id}/contract-summary")
    UserContractSummaryDTO getUserContractSummary(@PathVariable("id") Long id,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization);
}

