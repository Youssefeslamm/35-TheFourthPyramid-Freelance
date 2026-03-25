package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutPromo;
import com.team35.freelance.wallet.model.PromoCode;
import com.team35.freelance.wallet.repository.PayoutPromoRepository;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PayoutPromoService {

    private final PayoutPromoRepository payoutPromoRepository;
    private final PayoutRepository payoutRepository;
    private final PromoCodeRepository promoCodeRepository;

    public PayoutPromoService(PayoutPromoRepository payoutPromoRepository,
                              PayoutRepository payoutRepository,
                              PromoCodeRepository promoCodeRepository) {
        this.payoutPromoRepository = payoutPromoRepository;
        this.payoutRepository = payoutRepository;
        this.promoCodeRepository = promoCodeRepository;
    }

    public List<PayoutPromo> getAllPayoutPromos() {
        return payoutPromoRepository.findAll();
    }

    public PayoutPromo getPayoutPromoById(Long id) {
        return payoutPromoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PayoutPromo not found with id: " + id));
    }

    public PayoutPromo createPayoutPromo(Long payoutId, Long promoCodeId, Double discountApplied) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new RuntimeException("Payout not found with id: " + payoutId));

        PromoCode promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new RuntimeException("PromoCode not found with id: " + promoCodeId));

        PayoutPromo payoutPromo = new PayoutPromo();
        payoutPromo.setPayout(payout);
        payoutPromo.setPromoCode(promoCode);
        payoutPromo.setDiscountApplied(discountApplied);
        payoutPromo.setAppliedAt(LocalDateTime.now());

        return payoutPromoRepository.save(payoutPromo);
    }

    public PayoutPromo updatePayoutPromo(Long id, Double discountApplied) {
        PayoutPromo payoutPromo = payoutPromoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PayoutPromo not found with id: " + id));

        payoutPromo.setDiscountApplied(discountApplied);
        return payoutPromoRepository.save(payoutPromo);
    }

    public void deletePayoutPromo(Long id) {
        PayoutPromo payoutPromo = payoutPromoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PayoutPromo not found with id: " + id));

        payoutPromoRepository.delete(payoutPromo);
    }

    public List<PayoutPromo> getByPayoutId(Long payoutId) {
        return payoutPromoRepository.findByPayoutId(payoutId);
    }

    public List<PayoutPromo> getByPromoCodeId(Long promoCodeId) {
        return payoutPromoRepository.findByPromoCodeId(promoCodeId);
    }
}
