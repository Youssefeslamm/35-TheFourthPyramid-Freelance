package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.JobDTO;
import com.team35.freelance.contracts.dto.JobDashboardDTO;
import com.team35.freelance.contracts.dto.JobProposalSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "job-service", url = "${feign.job-service.url}")
public interface JobServiceClient {

    @GetMapping("/api/jobs/{id}")
    JobDTO getJobByIdInternal(@PathVariable("id") Long id,
                              @RequestHeader("X-Correlation-ID") String correlationId);

    @GetMapping("/api/jobs/{id}/proposal-summary")
    JobProposalSummaryDTO getProposalSummaryInternal(@PathVariable("id") Long id,
                                                     @RequestParam("startDate") String startDate,
                                                     @RequestParam("endDate") String endDate,
                                                     @RequestHeader("X-Correlation-ID") String correlationId);

    @GetMapping("/api/jobs/{id}/dashboard")
    JobDashboardDTO getJobDashboardInternal(@PathVariable("id") Long id,
                                            @RequestHeader("X-Correlation-ID") String correlationId);

    default JobDTO getJobById(Long id, String correlationId) {
        return FeignClientSupport.execute(
                "job-service",
                "getJobById",
                () -> getJobByIdInternal(id, correlationId),
                null
        );
    }

    default JobProposalSummaryDTO getProposalSummary(Long id, String startDate, String endDate, String correlationId) {
        return FeignClientSupport.execute(
                "job-service",
                "getProposalSummary",
                () -> getProposalSummaryInternal(id, startDate, endDate, correlationId),
                null
        );
    }

    default JobDashboardDTO getJobDashboard(Long id, String correlationId) {
        return FeignClientSupport.execute(
                "job-service",
                "getJobDashboard",
                () -> getJobDashboardInternal(id, correlationId),
                null
        );
    }
}
