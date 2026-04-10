package com.team35.freelance.job.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping; // Good to have, though Map is the main one needed here
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.team35.freelance.job.dto.JobProposalSummaryDTO;
import com.team35.freelance.job.dto.RateJobRequestDTO;
import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.service.JobService;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{id}/proposal-summary")
    public ResponseEntity<JobProposalSummaryDTO> getProposalSummary(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        return ResponseEntity.ok(jobService.getProposalSummary(id, startDate, endDate));
    }
    @GetMapping("/search")
    public ResponseEntity<List<Job>> searchJobs(
            @RequestParam(required = false) String status,
            @RequestParam Double minBudget,
            @RequestParam Double maxBudget) {

        return ResponseEntity.ok(jobService.searchJobs(status, minBudget, maxBudget));
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(job));
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        return ResponseEntity.ok(jobService.updateJob(id, job));
    }


    @PutMapping("/{id}/requirements")
    public ResponseEntity<Job> updateJobRequirements(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requirements) {

        Job updatedJob = jobService.updateRequirements(id, requirements);
        return ResponseEntity.ok(updatedJob);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

     @PostMapping("/{id}/rate")
    public ResponseEntity<Job> rateJob(
            @PathVariable Long id,
            @RequestBody RateJobRequestDTO request
    ) {
        return ResponseEntity.ok(jobService.rateJob(id, request));
    }
}