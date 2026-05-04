package com.team35.freelance.job.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.team35.freelance.job.dto.CloseJobRequest;
import com.team35.freelance.job.dto.JobAttachmentAlertDTO;
import com.team35.freelance.job.dto.JobDashboardDTO;
import com.team35.freelance.job.dto.JobProposalSummaryDTO;
import com.team35.freelance.job.dto.RateJobRequestDTO;
import com.team35.freelance.job.dto.TopBudgetJobDTO;
import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.service.JobService;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // --- M1/M2 Standard CRUD & Requirements ---

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/requirements")
    public ResponseEntity<Job> updateJobRequirements(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requirements) {
        return ResponseEntity.ok(jobService.updateRequirements(id, requirements));
    }

    // --- Specialized Queries & Reports ---

    @GetMapping("/search")
    public ResponseEntity<List<Job>> searchJobs(
            @RequestParam(required = false) String status,
            @RequestParam Double minBudget,
            @RequestParam Double maxBudget) {
        return ResponseEntity.ok(jobService.searchJobs(status, minBudget, maxBudget));
    }

    @GetMapping("/{id}/proposal-summary")
    public ResponseEntity<JobProposalSummaryDTO> getProposalSummary(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(jobService.getProposalSummary(id, startDate, endDate));
    }

    @GetMapping("/attachments/expired")
    public ResponseEntity<List<JobAttachmentAlertDTO>> getJobsWithExpiredAttachments() {
        return ResponseEntity.ok(jobService.getJobsWithExpiredAttachments());
    }

    @GetMapping("/reports/top-budget")
    public ResponseEntity<List<TopBudgetJobDTO>> getTopBudgetJobs(@RequestParam Integer limit) {
        return ResponseEntity.ok(jobService.getTopBudgetJobs(limit));
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<JobDashboardDTO> getJobDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobDashboard(id));
    }

    @GetMapping("/requirements/search")
    public ResponseEntity<List<Job>> filterJobsByRequirement(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(jobService.filterJobsByRequirement(key, value, status));
    }

    // --- Workflow Actions (Rating/Closing) ---

    @PostMapping("/{id}/rate")
    public ResponseEntity<Job> rateJob(
            @PathVariable Long id,
            @RequestBody RateJobRequestDTO request) {
        return ResponseEntity.ok(jobService.rateJob(id, request));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<Job> closeJob(
            @PathVariable Long id,
            @RequestBody CloseJobRequest request) {
        return ResponseEntity.ok(jobService.closeJob(id, request));
    }
}
