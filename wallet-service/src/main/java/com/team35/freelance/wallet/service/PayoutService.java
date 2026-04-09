package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutStatus;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team35.freelance.wallet.dto.ProcessPayoutRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.HashMap;
import java.util.Map;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;


// ✅ ADD THIS
import com.team35.freelance.wallet.dto.FreelancerPayoutSummaryDTO;

@Service
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final PromoCodeRepository promoCodeRepository;

    public PayoutService(PayoutRepository payoutRepository, PromoCodeRepository promoCodeRepository) {
        this.payoutRepository = payoutRepository;
        this.promoCodeRepository = promoCodeRepository;

    }

    // ---------------- EXISTING ----------------

    public Payout createPayout(Payout payout) {
        payout.setCreatedAt(LocalDateTime.now());
        return payoutRepository.save(payout);
    }

    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    public Payout getPayoutById(Long id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payout not found"));
    }

    public Payout updatePayout(Long id, Payout updatedPayout) {
        Payout existing = getPayoutById(id);

        existing.setContractId(updatedPayout.getContractId());
        existing.setFreelancerId(updatedPayout.getFreelancerId());
        existing.setAmount(updatedPayout.getAmount());
        existing.setMethod(updatedPayout.getMethod());
        existing.setStatus(updatedPayout.getStatus());
        existing.setTransactionDetails(updatedPayout.getTransactionDetails());

        return payoutRepository.save(existing);
    }

    public void deletePayout(Long id) {
        if (!payoutRepository.existsById(id)) {
            throw new RuntimeException("Payout not found");
        }
        payoutRepository.deleteById(id);
    }

    // ---------------- SUMMARY ----------------

    public FreelancerPayoutSummaryDTO getFreelancerSummary(Long freelancerId) {

        Object[] result = payoutRepository.getFreelancerPayoutSummary(freelancerId);

        if (result == null || result.length == 0) {
            return new FreelancerPayoutSummaryDTO(
                    freelancerId, 0L, 0L, 0L, 0L, 0.0, 0.0
            );
        }

        return new FreelancerPayoutSummaryDTO(
                ((Number) result[0]).longValue(),
                ((Number) result[1]).longValue(),
                ((Number) result[2]).longValue(),
                ((Number) result[3]).longValue(),
                ((Number) result[4]).longValue(),
                ((Number) result[5]).doubleValue(),
                ((Number) result[6]).doubleValue()
        );
    }

    // ---------------- NEW FEATURE ----------------

    @Transactional
    public Payout processContractPayout(Long contractId, ProcessPayoutRequest request) {

        // 1. Check contract exists
        String contractStatus = payoutRepository.getContractStatus(contractId);

        if (contractStatus == null) {
            throw new RuntimeException("Contract not found");
        }

        // 2. Validate contract status
        if (!contractStatus.equals("COMPLETED")) {
            throw new RuntimeException("Contract is not completed");
        }

        // 3. Get payout
        Payout payout = payoutRepository.findByContractId(contractId);

        if (payout == null) {
            throw new RuntimeException("Payout not found");
        }

        // 4. Check already paid
        if (payout.getStatus() == PayoutStatus.COMPLETED) {
            throw new RuntimeException("already paid");
        }

        // 5. Update payout
        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setMethod(request.getMethod());

        Map<String, Object> details = new HashMap<>();
        details.put("accountLastFour", request.getAccountLastFour());
        details.put("processedAt", LocalDateTime.now().toString());

        payout.setTransactionDetails(details);
    public List<Payout> searchPayouts(PayoutStatus status, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startDate != null) {
            startDateTime = startDate.atStartOfDay();
        }

        if (endDate != null) {
            endDateTime = endDate.atTime(23, 59, 59);
        }
        // CASE 1: no filters → return all
        if (status == null && startDateTime == null && endDateTime == null) {
            return payoutRepository.findAll();
        }
        // CASE 2: only status
        if (status != null && startDateTime == null && endDateTime == null) {
            return payoutRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        // CASE 3: only date range
        if (status == null) {
            return payoutRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
        }
        // CASE 4: both status + date
        return payoutRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                status, startDateTime, endDateTime
        );
    }

    public List<PromoCodeUsage> getMostUsedPromoCodes(int limit) {
        if (limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be greater than 0");
        }


        List<Object[]> rows = promoCodeRepository.findTopUsedPromoCodes(limit);
        List<PromoCodeUsage> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long promoCodeId = ((Number) row[0]).longValue();
            String code = (String) row[1];
            String discountType = row[2].toString();
            Double discountValue = ((Number) row[3]).doubleValue();
            Integer timesUsed = ((Number) row[4]).intValue();
            Double totalDiscountGiven = ((Number) row[5]).doubleValue();
            Boolean active = (Boolean) row[6];

            LocalDateTime expiryDate;
            Object expiryObj = row[7];
            if (expiryObj instanceof Timestamp) {
                expiryDate = ((Timestamp) expiryObj).toLocalDateTime();
            } else if (expiryObj instanceof LocalDateTime) {
                expiryDate = (LocalDateTime) expiryObj;
            } else {
                expiryDate = LocalDateTime.parse(expiryObj.toString());
            }

            Boolean expired = expiryDate.isBefore(LocalDateTime.now());

            PromoCodeUsage dto = new PromoCodeUsage(
                    promoCodeId,
                    code,
                    discountType,
                    discountValue,
                    timesUsed,
                    totalDiscountGiven,
                    active,
                    expired
            );

            result.add(dto);
        }

        return result;
    }
    @Transactional
    public Payout retryFailedPayout(Long id) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));

        if (payout.getStatus() != PayoutStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only failed payouts can be retried");
        }

        payout.setStatus(PayoutStatus.COMPLETED);
        return payoutRepository.save(payout);
    }
    public Payout processRefund(Long id, String reason) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));

        if (reason == null || reason.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund reason must not be blank");
        }

        if (payout.getStatus() != PayoutStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed payouts can be refunded");
        }

        payout.setStatus(PayoutStatus.REFUNDED);

        Map<String, Object> transactionDetails = payout.getTransactionDetails();
        if (transactionDetails == null) {
            transactionDetails = new HashMap<>();
        }

        Object retryValue = transactionDetails.get("retryAttempt");
        int retryAttempt = 0;

        if (retryValue instanceof Number) {
            retryAttempt = ((Number) retryValue).intValue();
        } else if (retryValue instanceof String) {
            try {
                retryAttempt = Integer.parseInt((String) retryValue);
            } catch (NumberFormatException e) {
                retryAttempt = 0;
            }
        }

        transactionDetails.put("retryAttempt", retryAttempt + 1);
        transactionDetails.put("gatewayResponse", "approved");
        transactionDetails.put("refundReason", reason);
        transactionDetails.put("refundedAt", LocalDateTime.now().toString());

        payout.setTransactionDetails(transactionDetails);

        return payoutRepository.save(payout);
    }
}