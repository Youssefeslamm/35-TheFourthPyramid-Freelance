package com.team35.freelance.job.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobHealthController {

    @GetMapping("/api/jobs/health")
    public String health() {
        return "OK";
    }
}