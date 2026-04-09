package com.team35.freelance.job.service;

import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    public List<Job> searchJobs(String status, Double minBudget, Double maxBudget) {
        // Requirement 9.2.1: Validate range (400 if min > max)
        if (minBudget > maxBudget) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Min budget cannot be greater than max");
        }

        return jobRepository.searchJobs(status, minBudget, maxBudget);
    }

    // Helper for Scenario (a): Saving jobs to the database
    public Job createJob(Job job) {
        return jobRepository.save(job);
    }
}