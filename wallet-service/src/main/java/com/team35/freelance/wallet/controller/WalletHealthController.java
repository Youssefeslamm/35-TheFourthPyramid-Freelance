package com.team35.freelance.wallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletHealthController {

    @GetMapping("/api/payouts/health")
    public String health() {
        return "OK";
    }
}