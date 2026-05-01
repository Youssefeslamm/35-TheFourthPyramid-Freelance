package com.team35.freelance.job.service;

import java.time.LocalDate;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.team35.freelance.job.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.transaction.annotation.Transactional;

import com.team35.freelance.job.exception.BadRequestException;
import com.team35.freelance.job.exception.ResourceNotFoundException;
import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobStatus;
import com.team35.freelance.job.repository.JobAttachmentRepository;
import com.team35.freelance.job.common.observer.EntityObserver;
import com.team35.freelance.job.common.observer.MongoEventLogger;
import com.team35.freelance.job.repository.JobRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;



@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobAttachmentRepository jobAttachmentRepository;
    private final List<EntityObserver> observers = new ArrayList<>();

    public JobService(JobRepository jobRepository,
                      JobAttachmentRepository jobAttachmentRepository,
                      MongoEventLogger mongoEventLogger) {

        this.jobRepository = jobRepository;
        this.jobAttachmentRepository = jobAttachmentRepository;

        this.observers.add(mongoEventLogger);
    }
    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }


    @Cacheable(value = "job-service::S2-F3", key = "#id + ':' + #startDate + ':' + #endDate")
    public JobProposalSummaryDTO getProposalSummary(Long id, String startDate, String endDate) {
        // 1. Check if job exists first for the 404 requirement
        if (!jobRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }

        // 2. Get the aggregated data
        Map<String, Object> result = jobRepository.getProposalSummaryRaw(id, startDate, endDate);

        // 3. Map the database results to the DTO
        return new JobProposalSummaryDTOBuilder()
                .jobId(((Number) result.get("jobid")).longValue())
                .title((String) result.get("title"))
                .totalProposals(((Number) result.get("totalproposals")).longValue())
                .averageBidAmount(((Number) result.get("averagebidamount")).doubleValue())
                .lowestBid(((Number) result.get("lowestbid")).doubleValue())
                .highestBid(((Number) result.get("highestbid")).doubleValue())
                .build();
    }

    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9"
    }, allEntries = true)
    public Job updateRequirements(Long id, Map<String, Object> incomingRequirements) {
        // 1. Find the job or throw 404
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        // 2. Ensure the map isn't null (just in case)
        if (job.getRequirements() == null) {
            job.setRequirements(new HashMap<>());
        }

        // 3. Merge the new requirements into the existing ones
        // putAll() adds new keys and overwrites existing matching keys
        job.getRequirements().putAll(incomingRequirements);

        // 4. Save and return
        return jobRepository.save(job);
    }


    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9"
    }, allEntries = true)
    public Job createJob(Job job) {
        validateJob(job);

        if (job.getStatus() == null) {
            job.setStatus(JobStatus.OPEN);
        }
        if (job.getRating() == null) {
            job.setRating(0.0);
        }
        if (job.getTotalRatings() == null) {
            job.setTotalRatings(0);
        }

        Job saved = jobRepository.save(job);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_CREATED");
        payload.put("jobId", saved.getId());

        notifyObservers("JOB_CREATED", payload);

        return saved;    }

    @Cacheable(value = "job-service::job", key = "#id")
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9"
    }, allEntries = true)
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

        if (updatedJob.getRating() != null) {
            existing.setRating(updatedJob.getRating());
        }

        if (updatedJob.getTotalRatings() != null) {
            existing.setTotalRatings(updatedJob.getTotalRatings());
        }

        existing.setRequirements(updatedJob.getRequirements());

        Job saved = jobRepository.save(existing);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_UPDATED");
        payload.put("jobId", saved.getId());

        notifyObservers("JOB_UPDATED", payload);

        return saved;    }

    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9"
    }, allEntries = true)
    public void deleteJob(Long id) {
        Job existing = getJobById(id);

        jobRepository.delete(existing); // 🔥 MISSING

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_DELETED");
        payload.put("jobId", id);

        notifyObservers("JOB_DELETED", payload);
    }

    @Cacheable(value = "job-service::S2-F1", key = "#status + ':' + #minBudget + ':' + #maxBudget")
    public List<Job> searchJobs(String status, Double minBudget, Double maxBudget) {
        if (minBudget == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minBudget is required");
        }

        if (maxBudget == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxBudget is required");
        }

        if (minBudget > maxBudget) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minBudget cannot be greater than maxBudget"
            );
        }

        return jobRepository.searchJobs(status, minBudget, maxBudget);
    }


    private void validateJob(Job job) {
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job request body is required");
        }

        if (job.getClientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId is required");
        }

        if (job.getTitle() == null || job.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }

        if (job.getDescription() == null || job.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        }

        if (job.getCategory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }

        if (job.getBudgetMin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budgetMin is required");
        }

        if (job.getBudgetMax() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budgetMax is required");
        }

        if (job.getBudgetMin() > job.getBudgetMax()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "budgetMin cannot be greater than budgetMax"
            );
        }
    }

    @Cacheable(value = "job-service::S2-F9", key = "'expired'")
    public List<JobAttachmentAlertDTO> getJobsWithExpiredAttachments() {
        List<JobAttachmentAlertDTO> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        jobRepository.findJobsWithExpiredAttachments().forEach(row -> {
            Job job = jobRepository.findByIdWithAttachments(row.getJobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

            List<JobAttachment> expiredAttachments =
                    jobAttachmentRepository.findByJobIdAndExpiryDateBefore(job.getId(), today);

            if (!expiredAttachments.isEmpty()) {
                alerts.add(
                        new JobAttachmentAlertDTOBuilder()
                                .jobId(job.getId())
                                .jobTitle(job.getTitle())
                                .jobStatus(job.getStatus())
                                .expiredAttachments(expiredAttachments)
                                .expiredCount(expiredAttachments.size())
                                .build()
                );
            }
        });

        return alerts;
    }
    @Transactional
    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9"
    }, allEntries = true)
    public Job rateJob(Long jobId, RateJobRequestDTO request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.getContractId() == null) {
            throw new BadRequestException("contractId is required");
        }
        if (request.getRating() == null) {
            throw new BadRequestException("rating is required");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("rating must be between 1 and 5 inclusive");
        }

        ContractLookupProjection contract = jobRepository.findContractById(request.getContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        if (!jobId.equals(contract.getJobId())) {
            throw new BadRequestException("Contract does not belong to this job");
        }

        if (!"COMPLETED".equalsIgnoreCase(contract.getStatus())) {
            throw new BadRequestException("Contract must be COMPLETED before rating");
        }

        double currentRating = job.getRating() == null ? 0.0 : job.getRating();
        int totalRatings = job.getTotalRatings() == null ? 0 : job.getTotalRatings();

        double newAverage =
                ((currentRating * totalRatings) + request.getRating())
                        / (totalRatings + 1);

        job.setRating(newAverage);
        job.setTotalRatings(totalRatings + 1);

        return jobRepository.save(job);
    }
    @Transactional
    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9"
    }, allEntries = true)
    public Job closeJob(Long id, CloseJobRequest request) {
        Job job = getJobById(id);

        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        if (request.getStatus() != JobStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be CLOSED");
        }

        if (jobRepository.existsActiveContractForJob(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot close job while an active contract exists"
            );
        }

        job.setStatus(JobStatus.CLOSED);
        jobRepository.rejectSubmittedProposalsForJob(id);

        Job saved = jobRepository.save(job); // 🔥 MISSING

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_CLOSED");
        payload.put("jobId", saved.getId());

        notifyObservers("JOB_CLOSED", payload);

        return saved; // 🔥 ALSO MISSING
        }


    @Cacheable(value = "job-service::S2-F5", key = "#key + ':' + #value + ':' + #status")
    public List<Job> filterJobsByRequirement(String key, String value, String status) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key is required");
        }

        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value is required");
        }

        return jobRepository.findByRequirementAndOptionalStatus(
                key.trim(),
                value.trim(),
                status
        );
    }


    @Cacheable(value = "job-service::S2-F6", key = "#limit")
    public List<TopBudgetJobDTO> getTopBudgetJobs(Integer limit) {
        if (limit == null || limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than 0");
        }

        List<Object[]> rows = jobRepository.findTopBudgetJobs(limit);
        List<TopBudgetJobDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long jobId = ((Number) row[0]).longValue();
            String title = (String) row[1];
            Double budgetMax = ((Number) row[2]).doubleValue();
            Long totalProposals = ((Number) row[3]).longValue();

            result.add(new TopBudgetJobDTO(jobId, title, budgetMax, totalProposals));
        }

        return result;
    }

}