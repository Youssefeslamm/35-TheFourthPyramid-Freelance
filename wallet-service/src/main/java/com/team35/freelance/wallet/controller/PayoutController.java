package com.team35.freelance.wallet.controller;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.service.PayoutService;
import com.team35.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team35.freelance.wallet.dto.ProcessPayoutRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payouts")
public class PayoutController {

    private final PayoutService payoutService;

    public PayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    // -------- CRUD --------

    @PostMapping
    public ResponseEntity<Payout> createPayout(@RequestBody Payout payout) {
        return ResponseEntity.ok(payoutService.createPayout(payout));
    }

    @GetMapping
    public ResponseEntity<List<Payout>> getAllPayouts() {
        return ResponseEntity.ok(payoutService.getAllPayouts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payout> getPayoutById(@PathVariable Long id) {
        return ResponseEntity.ok(payoutService.getPayoutById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Payout> updatePayout(@PathVariable Long id, @RequestBody Payout payout) {
        return ResponseEntity.ok(payoutService.updatePayout(id, payout));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePayout(@PathVariable Long id) {
        payoutService.deletePayout(id);
        return ResponseEntity.ok("Payout deleted successfully");
    }

    // -------- SUMMARY --------

    @GetMapping("/freelancers/{freelancerId}/summary")
    public ResponseEntity<FreelancerPayoutSummaryDTO> getSummary(
            @PathVariable Long freelancerId) {

        return ResponseEntity.ok(
                payoutService.getFreelancerSummary(freelancerId)
        );
    }

    // -------- NEW FEATURE --------

    @PostMapping("/contract/{contractId}")
    public ResponseEntity<Payout> processPayout(
            @PathVariable Long contractId,
            @RequestBody ProcessPayoutRequest request) {

        Payout payout = payoutService.processContractPayout(contractId, request);

        return ResponseEntity.status(201).body(payout);
    }
}