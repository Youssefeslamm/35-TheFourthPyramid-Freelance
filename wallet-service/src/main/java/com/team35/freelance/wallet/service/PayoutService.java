package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutStatus;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team35.freelance.wallet.dto.ProcessPayoutRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return payoutRepository.save(payout);
    }
}