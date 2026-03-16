package com.team35.freelance.contract.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContractHealthController {

    @GetMapping("/api/contracts/health")
    public String health() {
        return "OK";
    }
}