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

@Service
public class PayoutService {

    private final PayoutRepository payoutRepository;

    public PayoutService(PayoutRepository payoutRepository) {
        this.payoutRepository = payoutRepository;
    }

    // Create
    public Payout createPayout(Payout payout) {
        payout.setCreatedAt(LocalDateTime.now());
        return payoutRepository.save(payout);
    }

    // Read all
    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    // Read by ID
    public Payout getPayoutById(Long id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payout not found"));
    }

    // Update
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

    // Delete
    public void deletePayout(Long id) {
        if (!payoutRepository.existsById(id)) {
            throw new RuntimeException("Payout not found");
        }
        payoutRepository.deleteById(id);
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