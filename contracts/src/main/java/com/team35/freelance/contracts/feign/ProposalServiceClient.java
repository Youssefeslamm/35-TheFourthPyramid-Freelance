package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.JobProposalSummaryDTO;
import com.team35.freelance.contracts.dto.ProposalDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "proposal-service", url = "${feign.proposal-service.url}")
public interface ProposalServiceClient {

    @GetMapping("/api/proposals/{id}")
    ProposalDTO getProposalById(@PathVariable("id") Long id);

    @GetMapping("/api/proposals/job/{jobId}/summary")
    JobProposalSummaryDTO getJobProposalSummary(
            @PathVariable("jobId") Long jobId,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate
    );
}