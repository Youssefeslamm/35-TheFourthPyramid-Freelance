package com.team35.freelance.wallet.controller;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.service.PayoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team35.freelance.wallet.model.PayoutStatus;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import com.team35.freelance.wallet.dto.RefundRequest;

import java.util.List;

// ✅ ADD THIS
import com.team35.freelance.wallet.dto.FreelancerPayoutSummaryDTO;

@RestController
@RequestMapping("/api/payouts")
public class PayoutController {

    private final PayoutService payoutService;

    public PayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    // -------- EXISTING CRUD --------

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

    // -------- NEW FEATURE --------

    @GetMapping("/freelancers/{freelancerId}/summary")
    public ResponseEntity<FreelancerPayoutSummaryDTO> getSummary(
            @PathVariable Long freelancerId) {

        return ResponseEntity.ok(
                payoutService.getFreelancerSummary(freelancerId)
    @GetMapping("/search")
    public ResponseEntity<List<Payout>> searchPayouts(
            @RequestParam(required = false) PayoutStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                payoutService.searchPayouts(status, startDate, endDate)
        );
    }

    @PutMapping("/{id}/retry")
    public ResponseEntity<Payout> retryPayout(@PathVariable Long id) {
        return ResponseEntity.ok(payoutService.retryFailedPayout(id));
    }




    @PutMapping("/{id}/refund")
    public ResponseEntity<Payout> refundPayout(@PathVariable Long id,
                                               @RequestBody RefundRequest request) {
        return ResponseEntity.ok(payoutService.processRefund(id, request.getReason()));
    }
}