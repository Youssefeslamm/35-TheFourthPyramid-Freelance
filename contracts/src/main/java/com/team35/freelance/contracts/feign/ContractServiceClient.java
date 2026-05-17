package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.ContractDTO;
import com.team35.freelance.contracts.dto.FreelancerPerformanceDTO;
import com.team35.freelance.contracts.dto.UserContractSummaryDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "contract-service", url = "${feign.contract-service.url}")
public interface ContractServiceClient {

    @GetMapping("/api/contracts/{id}")
    ContractDTO getContractByIdInternal(@PathVariable("id") Long id,
                                        @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId);

    @GetMapping("/api/contracts/user/{userId}/active")
    ContractDTO getActiveContractForUserInternal(@PathVariable("userId") Long userId,
                                                 @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId);

    @GetMapping("/api/contracts/user/{userId}/summary")
    UserContractSummaryDTO getUserContractSummary(@PathVariable("userId") Long userId);

    @GetMapping("/api/contracts/user/{userId}/active-count")
    int getActiveContractCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/contracts/user/{userId}/completed-count")
    long getCompletedContractCount(@PathVariable("userId") Long userId);

    @GetMapping("/api/contracts/freelancer/{freelancerId}/summary")
    FreelancerPerformanceDTO getFreelancerPerformance(
            @PathVariable("freelancerId") Long freelancerId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);

    @GetMapping("/api/contracts/job/{jobId}/active-count")
    int getActiveContractCountForJob(@PathVariable("jobId") Long jobId);

    @GetMapping("/api/contracts/proposal/{proposalId}/active")
    ContractDTO getActiveContractForProposal(@PathVariable("proposalId") Long proposalId);

    @GetMapping("/api/contracts/{contractId}")
    ContractDTO getContract(@PathVariable("contractId") Long contractId);

    default ContractDTO getContractById(Long id) {
        return getContractById(id, null);
    }

    default ContractDTO getContractById(Long id, String correlationId) {
        return FeignClientSupport.execute(
                "contract-service",
                "getContractById",
                () -> getContractByIdInternal(id, correlationId),
                null
        );
    }

    default ContractDTO getActiveContractForUser(Long userId) {
        return getActiveContractForUser(userId, null);
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
