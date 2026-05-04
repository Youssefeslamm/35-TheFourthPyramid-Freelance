package com.team35.freelance.wallet.controller;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutStatus;

import com.team35.freelance.wallet.service.PayoutService;
import com.team35.freelance.wallet.service.PayoutPromoService;

import com.team35.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team35.freelance.wallet.dto.ProcessPayoutRequest;
import com.team35.freelance.wallet.dto.PayoutDetailsDTO;
import com.team35.freelance.wallet.dto.PromoCodeUsage;
import com.team35.freelance.wallet.dto.RefundRequest;
import com.team35.freelance.wallet.dto.RevenueReportDTO;
import com.team35.freelance.wallet.dto.PayoutMethodDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payouts")
public class PayoutController {

    private final PayoutService payoutService;
    private final PayoutPromoService payoutPromoService;

    public PayoutController(PayoutService payoutService,
                            PayoutPromoService payoutPromoService) {
        this.payoutService = payoutService;
        this.payoutPromoService = payoutPromoService;
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
    public ResponseEntity<Payout> updatePayout(@PathVariable Long id,
                                               @RequestBody Payout payout) {
        return ResponseEntity.ok(payoutService.updatePayout(id, payout));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePayout(@PathVariable Long id) {
        payoutService.deletePayout(id);
        return ResponseEntity.ok("Payout deleted successfully");
    }

    // -------- S5-F3: FREELANCER PAYOUT SUMMARY --------
    // Spec: GET /api/payouts/freelancer/{freelancerId}/summary

    @GetMapping("/freelancer/{freelancerId}/summary")
    public ResponseEntity<FreelancerPayoutSummaryDTO> getFreelancerSummary(
            @PathVariable Long freelancerId) {

        return ResponseEntity.ok(
                payoutService.getFreelancerSummary(freelancerId)
        );
    }

    // -------- SEARCH --------

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

    // -------- PROMO --------

    @PostMapping("/{payoutId}/promos/{promoCodeId}")
    public ResponseEntity<Payout> applyPromoCodeToPayout(
            @PathVariable Long payoutId,
            @PathVariable Long promoCodeId
    ) {
        return ResponseEntity.ok(
                payoutPromoService.applyPromoCodeToPayout(payoutId, promoCodeId)
        );
    }

    @GetMapping("/promos/top-used")
    public ResponseEntity<List<PromoCodeUsage>> getMostUsedPromoCodes(
            @RequestParam int limit) {

        return ResponseEntity.ok(
                payoutService.getMostUsedPromoCodes(limit)
        );
    }

    // -------- RETRY --------

    @PutMapping("/{id}/retry")
    public ResponseEntity<Payout> retryPayout(@PathVariable Long id) {
        return ResponseEntity.ok(
                payoutService.retryFailedPayout(id)
        );
    }

    // -------- REFUND --------

    @PutMapping("/{id}/refund")
    public ResponseEntity<Payout> refundPayout(@PathVariable Long id,
                                               @RequestBody RefundRequest request) {

        return ResponseEntity.ok(
                payoutService.processRefund(id, request.getReason())
        );
    }

    // -------- DETAILS --------

    @GetMapping("/{payoutId}/details")
    public ResponseEntity<PayoutDetailsDTO> getPayoutDetails(
            @PathVariable Long payoutId
    ) {
        return ResponseEntity.ok(
                payoutPromoService.getPayoutDetails(payoutId)
        );
    }

    // -------- PROCESS PAYOUT (S5-F4) --------

    @PostMapping("/contract/{contractId}")
    public ResponseEntity<Void> processContractPayout(
            @PathVariable Long contractId,
            @RequestBody ProcessPayoutRequest request
    ) {
        payoutService.processContractPayout(contractId, request);
        return ResponseEntity.status(201).build();
    }

    // -------- S5-F6: REVENUE REPORT --------

    @GetMapping("/reports/revenue")
    public ResponseEntity<RevenueReportDTO> getRevenueReport(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        return ResponseEntity.ok(
                payoutService.getRevenueReport(startDate, endDate)
        );
    }
    // S5-F11
    @GetMapping("/analytics/methods")
    public ResponseEntity<List<PayoutMethodDTO>> getPayoutMethodBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(payoutService.getPayoutMethodBreakdown(startDate, endDate));
    }
}