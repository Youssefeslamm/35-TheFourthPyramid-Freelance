package com.team35.freelance.job.service;

import com.team35.freelance.contracts.feign.ProposalServiceClient;
import com.team35.freelance.job.dto.JobDashboardDTO;
import com.team35.freelance.job.exception.ResourceNotFoundException;
import com.team35.freelance.job.repository.JobAttachmentRepository;
import com.team35.freelance.job.repository.JobRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JobDashboardCacheService {

    private static final Logger log = LoggerFactory.getLogger(JobDashboardCacheService.class);

    private final JobRepository jobRepository;
    private final JobAttachmentRepository jobAttachmentRepository;
    private final ProposalServiceClient proposalServiceClient;

    public JobDashboardCacheService(JobRepository jobRepository,
                                    JobAttachmentRepository jobAttachmentRepository,
                                    ProposalServiceClient proposalServiceClient) {
        this.jobRepository = jobRepository;
        this.jobAttachmentRepository = jobAttachmentRepository;
        this.proposalServiceClient = proposalServiceClient;
    }

    @Cacheable(value = "job-service::S2-F12", key = "#jobId")
    public JobDashboardDTO getJobDashboard(Long jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        com.team35.freelance.contracts.dto.JobProposalSummaryDTO proposalSummary;

        try {
            proposalSummary = proposalServiceClient.getJobProposalSummary(
                    jobId,
                    "1970-01-01",
                    "2999-12-31"
            );
        } catch (FeignException.NotFound e) {
            proposalSummary = new com.team35.freelance.contracts.dto.JobProposalSummaryDTO(
                    jobId,
                    job.getTitle(),
                    0L,
                    0L,
                    0.0,
                    0.0,
                    0.0
            );
        } catch (FeignException e) {
            log.warn("proposal-service unavailable while building dashboard for job {}: {}", jobId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Proposal service temporarily unavailable");
        }

        Long activeAttachments = jobAttachmentRepository.countActiveAttachmentsForJob(jobId);

        return JobDashboardDTO.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .totalProposals(proposalSummary.getTotalProposals() == null ? 0L : proposalSummary.getTotalProposals())
                .acceptedProposals(proposalSummary.getAcceptedProposals() == null ? 0L : proposalSummary.getAcceptedProposals())
                .averageBidAmount(proposalSummary.getAverageBidAmount() == null ? 0.0 : proposalSummary.getAverageBidAmount())
                .activeAttachments(activeAttachments)
                .rating(job.getRating() == null ? 0.0 : job.getRating())
                .build();
    }
}
