package com.team35.freelance.job.service;

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
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return JobDashboardDTO.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .totalProposals(0L)
                .acceptedProposals(0L)
                .averageBidAmount(0.0)
                .activeAttachments(0L)
                .rating(job.getRating() == null ? 0.0 : job.getRating())
                .build();
    }
}