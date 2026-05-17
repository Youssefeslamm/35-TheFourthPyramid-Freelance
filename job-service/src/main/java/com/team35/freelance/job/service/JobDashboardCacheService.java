package com.team35.freelance.job.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.team35.freelance.job.dto.JobDashboardDTO;
import com.team35.freelance.job.exception.ResourceNotFoundException;
import com.team35.freelance.job.repository.JobAttachmentRepository;
import com.team35.freelance.job.repository.JobRepository;

@Service
public class JobDashboardCacheService {

    private final JobRepository jobRepository;
    private final JobAttachmentRepository jobAttachmentRepository;

    public JobDashboardCacheService(JobRepository jobRepository,
                                    JobAttachmentRepository jobAttachmentRepository) {
        this.jobRepository = jobRepository;
        this.jobAttachmentRepository = jobAttachmentRepository;
    }

    @Cacheable(value = "job-service::S2-F12", key = "#jobId")
    public JobDashboardDTO getJobDashboard(Long jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        var statsRows = jobRepository.getJobDashboardProposalStats(jobId);

        Object[] stats = statsRows.isEmpty()
                ? new Object[] {0L, 0L, 0.0}
                : statsRows.get(0);

        Long totalProposals = toLong(stats[0]);
        Long acceptedProposals = toLong(stats[1]);
        Double averageBidAmount = toDouble(stats[2]);

        Long activeAttachments = jobAttachmentRepository.countActiveAttachmentsForJob(jobId);

        return JobDashboardDTO.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .totalProposals(totalProposals)
                .acceptedProposals(acceptedProposals)
                .averageBidAmount(averageBidAmount)
                .activeAttachments(activeAttachments)
                .rating(job.getRating() == null ? 0.0 : job.getRating())
                .build();
    }

    private static Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static Double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }
}