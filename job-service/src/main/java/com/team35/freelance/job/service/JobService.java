package com.team35.freelance.job.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.team35.freelance.job.common.observer.EntityObserver;
import com.team35.freelance.job.dto.*;
import com.team35.freelance.job.exception.BadRequestException;
import com.team35.freelance.job.exception.ResourceNotFoundException;
import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobStatus;
import com.team35.freelance.job.repository.JobAttachmentRepository;
import com.team35.freelance.job.repository.JobRepository;
import com.team35.freelance.job.service.EventFactory;
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobAttachmentRepository jobAttachmentRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final JobDashboardCacheService jobDashboardCacheService;
    private final JobIndexingService jobIndexingService;
    private final EventFactory eventFactory;

    public JobService(JobRepository jobRepository,
                      JobAttachmentRepository jobAttachmentRepository,
                      @Qualifier("jobServiceEventFactory") EventFactory eventFactory, // Resolved Conflict
                      JobDashboardCacheService jobDashboardCacheService,
                      JobIndexingService jobIndexingService) {

        this.jobRepository = jobRepository;
        this.jobAttachmentRepository = jobAttachmentRepository;
        this.eventFactory = eventFactory;
        this.jobDashboardCacheService = jobDashboardCacheService;
        this.jobIndexingService = jobIndexingService;
    }

    private void notifyObservers(String action, Long jobId, Object payload) {
        // We use our local eventFactory to create the MongoEvent
        // Since we are not using the generic 'mongoEventLogger' as an observer anymore
        // but rather a structured Factory, you can call it directly or keep using an observer list.
        // For your current structure, we'll implement the event creation here:
        eventFactory.createEvent(jobId, action, payload);
    }

    // --- CRUD OPERATIONS ---

    @CacheEvict(value = {"job-service::job","job-service::S2-F1","job-service::S2-F3","job-service::S2-F5","job-service::S2-F6","job-service::S2-F9","job-service::S2-F12"}, allEntries = true)
    public Job createJob(Job job) {
        validateJob(job);
        if (job.getStatus() == null) job.setStatus(JobStatus.OPEN);
        if (job.getRating() == null) job.setRating(0.0);
        if (job.getTotalRatings() == null) job.setTotalRatings(0);

        Job saved = jobRepository.save(job);
        jobIndexingService.indexJob(saved.getId());

        notifyObservers("JOB_CREATED", saved.getId(), "Job created successfully");
        return saved;
    }

    @Cacheable(value = "job-service::job", key = "#id")
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @CacheEvict(value = {"job-service::job","job-service::S2-F1","job-service::S2-F3","job-service::S2-F5","job-service::S2-F6","job-service::S2-F9","job-service::S2-F12"}, allEntries = true)
    public Job updateJob(Long id, Job updatedJob) {
        Job existing = getJobById(id);
        validateJob(updatedJob);

        existing.setClientId(updatedJob.getClientId());
        existing.setTitle(updatedJob.getTitle());
        existing.setDescription(updatedJob.getDescription());
        existing.setCategory(updatedJob.getCategory());
        existing.setStatus(updatedJob.getStatus());
        existing.setBudgetMin(updatedJob.getBudgetMin());
        existing.setBudgetMax(updatedJob.getBudgetMax());
        existing.setRequirements(updatedJob.getRequirements());

        Job saved = jobRepository.save(existing);
        jobIndexingService.indexJob(saved.getId());

        notifyObservers("JOB_UPDATED", saved.getId(), "Job details updated");
        return saved;
    }

    @CacheEvict(value = {"job-service::job","job-service::S2-F1","job-service::S2-F3","job-service::S2-F5","job-service::S2-F6","job-service::S2-F9","job-service::S2-F12"}, allEntries = true)
    public void deleteJob(Long id) {
        Job existing = getJobById(id);
        jobRepository.delete(existing);
        jobIndexingService.removeJobFromIndex(id);

        notifyObservers("JOB_DELETED", id, "Job removed from system");
    }

    // --- SPECIALIZED FEATURES ---

    @Cacheable(value = "job-service::S2-F9", key = "'expired'")
    public List<JobAttachmentAlertDTO> getJobsWithExpiredAttachments() {
        List<JobAttachmentAlertDTO> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        jobRepository.findJobsWithExpiredAttachments().forEach(row -> {
            Job job = jobRepository.findById(row.getJobId()).orElse(null);
            if (job != null) {
                List<JobAttachment> expired = jobAttachmentRepository.findByJobIdAndExpiryDateBefore(job.getId(), today);
                if (!expired.isEmpty()) {
                    alerts.add(JobAttachmentAlertDTO.builder()
                            .jobId(job.getId()).jobTitle(job.getTitle())
                            .jobStatus(job.getStatus()).expiredAttachments(expired)
                            .expiredCount(expired.size()).build());
                }
            }
        });
        return alerts;
    }

    @Cacheable(value = "job-service::S2-F6", key = "#limit")
    public List<TopBudgetJobDTO> getTopBudgetJobs(Integer limit) {
        if (limit == null || limit <= 0) throw new BadRequestException("Limit must be > 0");
        List<Object[]> rows = jobRepository.findTopBudgetJobs(limit);
        List<TopBudgetJobDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new TopBudgetJobDTO(((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).doubleValue(), ((Number) row[3]).longValue()));
        }
        return result;
    }

    public JobDashboardDTO getJobDashboard(Long id) {
        if (!jobRepository.existsById(id)) throw new ResourceNotFoundException("Job not found");
        return jobDashboardCacheService.getJobDashboard(id);
    }

    @Cacheable(value = "job-service::S2-F5", key = "#key + ':' + #value + ':' + #status")
    public List<Job> filterJobsByRequirement(String key, String value, String status) {
        if (key == null || value == null) throw new BadRequestException("Key and Value required");
        return jobRepository.findByRequirementAndOptionalStatus(key.trim(), value.trim(), status);
    }

    @Transactional
    public Job rateJob(Long jobId, RateJobRequestDTO request) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        double currentRating = job.getRating() == null ? 0.0 : job.getRating();
        int total = job.getTotalRatings() == null ? 0 : job.getTotalRatings();
        job.setRating(((currentRating * total) + request.getRating()) / (total + 1));
        job.setTotalRatings(total + 1);
        Job saved = jobRepository.save(job);
        jobIndexingService.indexJob(saved.getId());
        return saved;
    }

    @Transactional
    public Job closeJob(Long id, CloseJobRequest request) {
        Job job = getJobById(id);
        if (request.getStatus() != JobStatus.CLOSED) throw new BadRequestException("Status must be CLOSED");
        job.setStatus(JobStatus.CLOSED);
        Job saved = jobRepository.save(job);
        jobIndexingService.indexJob(saved.getId());
        return saved;
    }

    @Cacheable(value = "job-service::S2-F3", key = "#id + ':' + #startDate + ':' + #endDate")
    public JobProposalSummaryDTO getProposalSummary(Long id, String startDate, String endDate) {
        Map<String, Object> result = jobRepository.getProposalSummaryRaw(id, startDate, endDate);
        return new JobProposalSummaryDTOBuilder()
                .jobId(((Number) result.get("jobid")).longValue())
                .title((String) result.get("title"))
                .totalProposals(((Number) result.get("totalproposals")).longValue())
                .averageBidAmount(((Number) result.get("averagebidamount")).doubleValue())
                .lowestBid(((Number) result.get("lowestbid")).doubleValue())
                .highestBid(((Number) result.get("highestbid")).doubleValue())
                .build();
    }

    public List<Job> searchJobs(String status, Double min, Double max) {
        return jobRepository.searchJobs(status, min, max);
    }

    @CacheEvict(value = {"job-service::job"}, allEntries = true)
    public Job updateRequirements(Long id, Map<String, Object> incoming) {
        Job job = getJobById(id);
        if (job.getRequirements() == null) job.setRequirements(new HashMap<>());
        job.getRequirements().putAll(incoming);
        Job saved = jobRepository.save(job);
        jobIndexingService.indexJob(saved.getId());
        return saved;
    }

    private void validateJob(Job job) {
        if (job == null || job.getTitle() == null || job.getBudgetMin() == null || job.getBudgetMax() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields");
        }
    }
}