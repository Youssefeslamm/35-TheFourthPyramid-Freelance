package com.team35.freelance.proposal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProposalHealthController {

    @GetMapping("/api/proposals/health")
    public String health() {
        return "OK";
    }
}