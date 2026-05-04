package com.team35.freelance.job.controller;


import com.team35.freelance.job.service.JobIndexingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobIndexingController {

    @Autowired
    private JobIndexingService indexingService;

    @PostMapping("/{id}/index")
    public ResponseEntity<String> indexJob(@PathVariable Long id) {
        indexingService.indexJob(id);
        return ResponseEntity.ok("Job indexed successfully.");
    }
}