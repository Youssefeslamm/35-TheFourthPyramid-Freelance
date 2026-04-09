package com.team35.freelance.job.controller;

import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    // Feature Endpoint: GET /api/jobs/search
    @GetMapping("/search")
    public ResponseEntity<List<Job>> searchJobs(
            @RequestParam(required = false) String status,
            @RequestParam Double minBudget,
            @RequestParam Double maxBudget) {

        List<Job> results = jobService.searchJobs(status, minBudget, maxBudget);
        return ResponseEntity.ok(results);
    }

    // Endpoint to set up your test data (Scenario a)
    @PostMapping
    public ResponseEntity<Job> addJob(@RequestBody Job job) {
        return ResponseEntity.ok(jobService.createJob(job));
    }
}