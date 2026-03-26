package com.team35.freelance.wallet.controller;

import com.team35.freelance.wallet.model.PayoutPromo;
import com.team35.freelance.wallet.service.PayoutPromoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payout-promos")
public class PayoutPromoController {

    private final PayoutPromoService payoutPromoService;

    public PayoutPromoController(PayoutPromoService payoutPromoService) {
        this.payoutPromoService = payoutPromoService;
    }

    // GET all
    @GetMapping
    public ResponseEntity<List<PayoutPromo>> getAll() {
        return ResponseEntity.ok(payoutPromoService.getAllPayoutPromos());
    }

    // GET by ID
    @GetMapping("/{id}")
    public ResponseEntity<PayoutPromo> getById(@PathVariable Long id) {
        return ResponseEntity.ok(payoutPromoService.getPayoutPromoById(id));
    }

    // GET all promos for a specific payout
    @GetMapping("/payout/{payoutId}")
    public ResponseEntity<List<PayoutPromo>> getByPayoutId(@PathVariable Long payoutId) {
        return ResponseEntity.ok(payoutPromoService.getByPayoutId(payoutId));
    }

    // GET all promos for a specific promo code
    @GetMapping("/promo-code/{promoCodeId}")
    public ResponseEntity<List<PayoutPromo>> getByPromoCodeId(@PathVariable Long promoCodeId) {
        return ResponseEntity.ok(payoutPromoService.getByPromoCodeId(promoCodeId));
    }

    // POST create
    @PostMapping
    public ResponseEntity<PayoutPromo> create(
            @RequestParam Long payoutId,
            @RequestParam Long promoCodeId,
            @RequestParam Double discountApplied) {
        PayoutPromo created = payoutPromoService.createPayoutPromo(payoutId, promoCodeId, discountApplied);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT update
    @PutMapping("/{id}")
    public ResponseEntity<PayoutPromo> update(
            @PathVariable Long id,
            @RequestParam Double discountApplied) {
        return ResponseEntity.ok(payoutPromoService.updatePayoutPromo(id, discountApplied));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        payoutPromoService.deletePayoutPromo(id);
        return ResponseEntity.noContent().build();
    }
}