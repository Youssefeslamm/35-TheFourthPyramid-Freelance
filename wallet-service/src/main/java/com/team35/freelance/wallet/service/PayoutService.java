package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutStatus;
import com.team35.freelance.wallet.repository.PayoutRepository;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// ✅ ADD THIS
import com.team35.freelance.wallet.dto.FreelancerPayoutSummaryDTO;

@Service
public class PayoutService {

    private final PayoutRepository payoutRepository;

    public PayoutService(PayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

    // ---------------- EXISTING CODE (UNCHANGED) ----------------

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

    // ---------------- NEW FEATURE ----------------

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

    @Transactional
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

        transactionDetails.put("refundReason", reason);
        transactionDetails.put("refundedAt", LocalDateTime.now().toString());

        payout.setTransactionDetails(transactionDetails);

        return payoutRepository.save(payout);
    }


}