package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.JobDTO;
import com.team35.freelance.contracts.dto.JobDashboardDTO;
import com.team35.freelance.contracts.dto.JobProposalSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "job-service", url = "${feign.job-service.url}")
public interface JobServiceClient {

    @GetMapping("/api/jobs/{id}")
    JobDTO getJobById(@PathVariable("id") Long id);

    @GetMapping("/api/jobs/{id}/proposal-summary")
    JobProposalSummaryDTO getProposalSummary(@PathVariable("id") Long id,
                                             @RequestParam("startDate") String startDate,
                                             @RequestParam("endDate") String endDate);

    @GetMapping("/api/jobs/{id}/dashboard")
    JobDashboardDTO getJobDashboard(@PathVariable("id") Long id);
}

