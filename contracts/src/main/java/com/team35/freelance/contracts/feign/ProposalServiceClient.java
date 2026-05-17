package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.JobProposalSummaryDTO;
import com.team35.freelance.contracts.dto.ProposalDTO;
import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "proposal-service", url = "${feign.proposal-service.url}")
public interface ProposalServiceClient {

    @GetMapping("/api/proposals/{id}")
    ProposalDTO getProposalById(@PathVariable("id") Long id);

    /**
     * S4S2 — Get proposal statistics for a job within a date range.
     * Calls GET /api/proposals/summary?jobId=X&startDate=Y&endDate=Z
     */
    @GetMapping("/api/proposals/summary")
    JobProposalSummaryDTO getJobProposalSummary(
            @RequestParam("jobId")    Long jobId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate")   String endDate
    );
}