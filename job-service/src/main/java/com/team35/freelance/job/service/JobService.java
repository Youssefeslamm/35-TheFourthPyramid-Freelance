package com.team35.freelance.job.service;

import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobStatus;
import com.team35.freelance.job.repository.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

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

        return jobRepository.save(job);
    }

    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

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

        return jobRepository.save(existing);
    }

    public void deleteJob(Long id) {
        Job existing = getJobById(id);
        jobRepository.delete(existing);
    }

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

        String normalizedStatus = normalizeStatus(status);
        return jobRepository.searchJobs(normalizedStatus, minBudget, maxBudget);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toUpperCase();

        boolean valid = Arrays.stream(JobStatus.values())
                .anyMatch(value -> value.name().equals(normalized));

        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid job status");
        }

        return normalized;
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
}