package com.team35.freelance.wallet.controller;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.service.PayoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.team35.freelance.wallet.model.PayoutStatus;
import org.springframework.format.annotation.DateTimeFormat;
import com.team35.freelance.wallet.service.PayoutPromoService;
import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/payouts")
public class PayoutController {

    private final PayoutService payoutService;
    private final PayoutPromoService payoutPromoService;

    public PayoutController(PayoutService payoutService, PayoutPromoService payoutPromoService) {
        this.payoutService = payoutService;
        this.payoutPromoService = payoutPromoService;
    }



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
    @PostMapping("/{payoutId}/promos/{promoCodeId}")
    public ResponseEntity<Payout> applyPromoCodeToPayout(
            @PathVariable("payoutId") Long payoutId,
            @PathVariable("promoCodeId") Long promoCodeId
    ) {
        return ResponseEntity.ok(
                payoutPromoService.applyPromoCodeToPayout(payoutId, promoCodeId)
        );
    }
}