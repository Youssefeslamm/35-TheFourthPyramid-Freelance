package com.team35.freelance.job.service;

import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.team35.freelance.job.dto.JobDashboardDTO;
import com.team35.freelance.job.exception.ResourceNotFoundException;
import com.team35.freelance.job.repository.JobRepository;

@Service
public class JobDashboardCacheService {

    private final JobRepository jobRepository;

    public JobDashboardCacheService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Cacheable(value = "job-service::S2-F12", key = "#jobId")
    public JobDashboardDTO getJobDashboard(Long jobId) {
        Map<String, Object> row = jobRepository.getJobDashboardRaw(jobId);

        if (row == null || row.isEmpty()) {
            throw new ResourceNotFoundException("Job not found");
        }

        return JobDashboardDTO.builder()
                .jobId(asLong(row.get("job_id")))
                .title((String) row.get("title"))
                .totalProposals(asLong(row.get("total_proposals")))
                .acceptedProposals(asLong(row.get("accepted_proposals")))
                .averageBidAmount(asDouble(row.get("average_bid_amount")))
                .activeAttachments(asLong(row.get("active_attachments")))
                .rating(asDouble(row.get("rating")))
                .build();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return ((Number) value).longValue();
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        return ((Number) value).doubleValue();
    }
}