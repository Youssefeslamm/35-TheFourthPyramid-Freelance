package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.model.*;
import com.team35.freelance.wallet.repository.PayoutPromoRepository;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.repository.PromoCodeRepository;
import com.team35.freelance.wallet.dto.PayoutDetailsDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

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
    //S5-F5
    @Transactional
    public Payout applyPromoCodeToPayout(Long payoutId, Long promoCodeId) {

        // a) Find payout
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found with id: " + payoutId));

        // b) Validate payout status
        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot apply promo code to a completed/cancelled payout");
        }

        // c) Find promo code
        PromoCode promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promocode not found with id: " + promoCodeId));

        // d) Validate promo code usability
        if (!Boolean.TRUE.equals(promoCode.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promo code is inactive");
        }

        if (promoCode.getExpiryDate().isBefore(LocalDateTime.now())
                || promoCode.getExpiryDate().isEqual(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promo code is expired");
        }

        if (promoCode.getCurrentUses() >= promoCode.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promo code usage limit reached");
        }

        // e) Check duplicate
        if (payoutPromoRepository.findByPayoutIdAndPromoCodeId(payoutId, promoCodeId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promo code already applied");
        }

        // f) Calculate discount
        double discount;
        if (promoCode.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = payout.getAmount() * promoCode.getDiscountValue() / 100.0;
        } else {
            discount = promoCode.getDiscountValue();
        }

        if (discount > payout.getAmount()) {
            discount = payout.getAmount();
        }

        // g) Create join entity
        PayoutPromo payoutPromo = new PayoutPromo();
        payoutPromo.setPayout(payout);
        payoutPromo.setPromoCode(promoCode);
        payoutPromo.setDiscountApplied(discount);
        payoutPromo.setAppliedAt(LocalDateTime.now());

        // h) Update promo usage
        promoCode.setCurrentUses(promoCode.getCurrentUses() + 1);

        // i) Save both
        payoutPromoRepository.save(payoutPromo);
        promoCodeRepository.save(promoCode);

        // keep in-memory relationship updated
        payout.getPayoutPromos().add(payoutPromo);

        // j) Return updated payout
        return payout;
    }
    public PayoutDetailsDTO getPayoutDetails(Long payoutId) {

        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payout not found with id: " + payoutId
                ));

        List<PayoutPromo> payoutPromos = payoutPromoRepository.findByPayoutId(payoutId);

        List<PayoutDetailsDTO.AppliedPromoCodeDTO> appliedPromoCodes = new ArrayList<>();
        double totalDiscount = 0.0;

        for (PayoutPromo payoutPromo : payoutPromos) {
            PromoCode promoCode = payoutPromo.getPromoCode();

            appliedPromoCodes.add(new PayoutDetailsDTO.AppliedPromoCodeDTO(
                    promoCode.getCode(),
                    promoCode.getDiscountType().name(),
                    payoutPromo.getDiscountApplied(),
                    payoutPromo.getAppliedAt()
            ));

            totalDiscount += payoutPromo.getDiscountApplied();
        }

        double finalAmount = payout.getAmount() - totalDiscount;

        return new PayoutDetailsDTO(
                payout.getId(),
                payout.getContractId(),
                payout.getFreelancerId(),
                payout.getAmount(),
                payout.getMethod(),
                payout.getStatus(),
                payout.getTransactionDetails(),
                appliedPromoCodes,
                totalDiscount,
                finalAmount
        );
    }
}
