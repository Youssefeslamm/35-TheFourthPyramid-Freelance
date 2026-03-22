package com.team35.freelance.wallet.controller;

import com.team35.freelance.wallet.model.PromoCode;
import com.team35.freelance.wallet.service.PromoCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promocodes")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    public PromoCodeController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @PostMapping
    public ResponseEntity<PromoCode> createPromoCode(@RequestBody PromoCode promoCode) {
        PromoCode createdPromoCode = promoCodeService.createPromoCode(promoCode);
        return new ResponseEntity<>(createdPromoCode, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PromoCode>> getAllPromoCodes() {
        return ResponseEntity.ok(promoCodeService.getAllPromoCodes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromoCode> getPromoCodeById(@PathVariable Long id) {
        return ResponseEntity.ok(promoCodeService.getPromoCodeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromoCode> updatePromoCode(@PathVariable Long id,
                                                     @RequestBody PromoCode promoCode) {
        return ResponseEntity.ok(promoCodeService.updatePromoCode(id, promoCode));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePromoCode(@PathVariable Long id) {
        promoCodeService.deletePromoCode(id);
        return ResponseEntity.ok("Promo code deleted successfully");
    }
}