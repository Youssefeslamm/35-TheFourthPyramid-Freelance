package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.model.*;
import com.team35.freelance.wallet.repository.PayoutPromoRepository;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.repository.PromoCodeRepository;
import com.team35.freelance.wallet.dto.PayoutDetailsDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "wallet-service::payout-promo", key = "#id")
    public PayoutPromo getPayoutPromoById(Long id) {
        return payoutPromoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PayoutPromo not found with id: " + id));
    }

    @CacheEvict(value = {
            "wallet-service::payout",
            "wallet-service::promo-code",
            "wallet-service::payout-promo",
            "wallet-service::S5-F1",
            "wallet-service::S5-F3",
            "wallet-service::S5-F6",
            "wallet-service::S5-F8",
            "wallet-service::S5-F9"
    }, allEntries = true)
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

    @CacheEvict(value = {
            "wallet-service::payout",
            "wallet-service::promo-code",
            "wallet-service::payout-promo",
            "wallet-service::S5-F1",
            "wallet-service::S5-F3",
            "wallet-service::S5-F6",
            "wallet-service::S5-F8",
            "wallet-service::S5-F9"
    }, allEntries = true)
    public PayoutPromo updatePayoutPromo(Long id, Double discountApplied) {
        PayoutPromo payoutPromo = payoutPromoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PayoutPromo not found with id: " + id));

        payoutPromo.setDiscountApplied(discountApplied);
        return payoutPromoRepository.save(payoutPromo);
    }

    @CacheEvict(value = {
            "wallet-service::payout",
            "wallet-service::promo-code",
            "wallet-service::payout-promo",
            "wallet-service::S5-F1",
            "wallet-service::S5-F3",
            "wallet-service::S5-F6",
            "wallet-service::S5-F8",
            "wallet-service::S5-F9"
    }, allEntries = true)
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

    @Transactional
    @CacheEvict(value = {
            "wallet-service::payout",
            "wallet-service::promo-code",
            "wallet-service::payout-promo",
            "wallet-service::S5-F1",
            "wallet-service::S5-F3",
            "wallet-service::S5-F6",
            "wallet-service::S5-F8",
            "wallet-service::S5-F9"
    }, allEntries = true)
    public Payout applyPromoCodeToPayout(Long payoutId, Long promoCodeId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found with id: " + payoutId));

        if (payout.getStatus() != PayoutStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot apply promo code to a completed/cancelled payout");
        }

        PromoCode promoCode = promoCodeRepository.findById(promoCodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promocode not found with id: " + promoCodeId));

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

        if (payoutPromoRepository.findByPayoutIdAndPromoCodeId(payoutId, promoCodeId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "promo code already applied");
        }

        double discount;

        if (promoCode.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = payout.getAmount() * promoCode.getDiscountValue() / 100.0;
        } else {
            discount = promoCode.getDiscountValue();
        }

        if (discount > payout.getAmount()) {
            discount = payout.getAmount();
        }

        PayoutPromo payoutPromo = new PayoutPromo();
        payoutPromo.setPayout(payout);
        payoutPromo.setPromoCode(promoCode);
        payoutPromo.setDiscountApplied(discount);
        payoutPromo.setAppliedAt(LocalDateTime.now());

        promoCode.setCurrentUses(promoCode.getCurrentUses() + 1);

        payoutPromoRepository.save(payoutPromo);
        promoCodeRepository.save(promoCode);

        payout.getPayoutPromos().add(payoutPromo);

        return payout;
    }

    @Cacheable(value = "wallet-service::S5-F8", key = "#payoutId")
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

            appliedPromoCodes.add(
                    PayoutDetailsDTO.AppliedPromoCodeDTO.builder()
                            .promoCode(promoCode.getCode())
                            .discountType(promoCode.getDiscountType().name())
                            .discountApplied(payoutPromo.getDiscountApplied())
                            .appliedAt(payoutPromo.getAppliedAt())
                            .build()
            );

            totalDiscount += payoutPromo.getDiscountApplied();
        }

        double finalAmount = payout.getAmount() - totalDiscount;

        return PayoutDetailsDTO.builder()
                .payoutId(payout.getId())
                .contractId(payout.getContractId())
                .freelancerId(payout.getFreelancerId())
                .originalAmount(payout.getAmount())
                .method(payout.getMethod())
                .status(payout.getStatus())
                .transactionDetails(payout.getTransactionDetails())
                .appliedPromoCodes(appliedPromoCodes)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .build();
    }
}