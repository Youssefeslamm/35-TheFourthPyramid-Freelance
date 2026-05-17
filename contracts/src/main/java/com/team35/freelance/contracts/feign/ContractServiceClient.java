package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.ContractDTO;
import com.team35.freelance.contracts.dto.UserContractSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "contract-service", url = "${feign.contract-service.url}")
public interface ContractServiceClient {

    @GetMapping("/api/contracts/{id}")
    ContractDTO getContractById(@PathVariable("id") Long id);

    @GetMapping("/api/contracts/user/{userId}/active")
    ContractDTO getActiveContractForUser(@PathVariable("userId") Long userId);

    @GetMapping("/api/contracts/user/{userId}/summary")
    UserContractSummaryDTO getUserContractSummary(@PathVariable("userId") Long userId);

    @GetMapping("/api/contracts/user/{userId}/active-count")
    int getActiveContractCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/contracts/user/{userId}/completed-count")
    long getCompletedContractCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/contracts/job/{jobId}/active-count")
    int getActiveContractCountForJob(@PathVariable("jobId") Long jobId);

    @GetMapping("/api/contracts/proposal/{proposalId}/active")
    ContractDTO getActiveContractForProposal(@PathVariable("proposalId") Long proposalId);
}
