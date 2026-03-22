package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.model.PromoCode;
import com.team35.freelance.wallet.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    public PromoCodeService(PromoCodeRepository promoCodeRepository) {
        this.promoCodeRepository = promoCodeRepository;
    }

    public PromoCode createPromoCode(PromoCode promoCode) {
        if (promoCodeRepository.existsByCode(promoCode.getCode())) {
            throw new RuntimeException("Promo code with this code already exists");
        }

        if (promoCode.getCurrentUses() == null) {
            promoCode.setCurrentUses(0);
        }

        if (promoCode.getActive() == null) {
            promoCode.setActive(true);
        }

        return promoCodeRepository.save(promoCode);
    }

    public List<PromoCode> getAllPromoCodes() {
        return promoCodeRepository.findAll();
    }

    public PromoCode getPromoCodeById(Long id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promo code not found with id: " + id));
    }

    public PromoCode updatePromoCode(Long id, PromoCode updatedPromoCode) {
        PromoCode existingPromoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promo code not found with id: " + id));

        if (!existingPromoCode.getCode().equals(updatedPromoCode.getCode())
                && promoCodeRepository.existsByCode(updatedPromoCode.getCode())) {
            throw new RuntimeException("Promo code with this code already exists");
        }

        existingPromoCode.setCode(updatedPromoCode.getCode());
        existingPromoCode.setDiscountType(updatedPromoCode.getDiscountType());
        existingPromoCode.setDiscountValue(updatedPromoCode.getDiscountValue());
        existingPromoCode.setMaxUses(updatedPromoCode.getMaxUses());
        existingPromoCode.setCurrentUses(updatedPromoCode.getCurrentUses());
        existingPromoCode.setExpiryDate(updatedPromoCode.getExpiryDate());
        existingPromoCode.setActive(updatedPromoCode.getActive());
        existingPromoCode.setMetadata(updatedPromoCode.getMetadata());

        return promoCodeRepository.save(existingPromoCode);
    }

    public void deletePromoCode(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promo code not found with id: " + id));

        promoCodeRepository.delete(promoCode);
    }
}