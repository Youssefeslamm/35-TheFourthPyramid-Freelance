package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutStatus;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.repository.PromoCodeRepository;

import com.team35.freelance.wallet.dto.FreelancerPayoutSummaryDTO;
import com.team35.freelance.wallet.dto.ProcessPayoutRequest;
import com.team35.freelance.wallet.dto.RevenueReportDTO;
import com.team35.freelance.wallet.dto.PromoCodeUsage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;

import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final PromoCodeRepository promoCodeRepository;

    public PayoutService(PayoutRepository payoutRepository,
                         PromoCodeRepository promoCodeRepository) {
        this.payoutRepository = payoutRepository;
        this.promoCodeRepository = promoCodeRepository;
    }

    // ---------------- CRUD ----------------

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

    public FreelancerPayoutSummaryDTO getFreelancerSummary(Long freelancerId) {

        List<Object[]> methodRows = payoutRepository.getFreelancerMethodBreakdown(freelancerId);

        Map<String, Double> methodBreakdown = new HashMap<>();
        long totalPayouts = 0L;
        double totalAmount = 0.0;

        for (Object[] row : methodRows) {
            String method = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            Double amount = ((Number) row[2]).doubleValue();

            methodBreakdown.put(method, amount);
            totalPayouts += count;
            totalAmount += amount;
        }

        return new FreelancerPayoutSummaryDTO(
                freelancerId,
                totalPayouts,
                totalAmount,
                methodBreakdown
        );
    }

    // ---------------- S5-F4: PROCESS PAYOUT ----------------

    @Transactional
    public Payout processContractPayout(Long contractId, ProcessPayoutRequest request) {

        String contractStatus = payoutRepository.getContractStatus(contractId);

        if (contractStatus == null) {
            throw new RuntimeException("Contract not found");
        }

        if (!PayoutStatus.COMPLETED.name().equals(contractStatus)) {
            throw new RuntimeException("Contract is not completed");
        }

        Payout payout = payoutRepository.findByContractId(contractId);

        if (payout == null) {
            throw new RuntimeException("Payout not found");
        }

        if (payout.getStatus() == PayoutStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setMethod(request.getMethod());

        Map<String, Object> details = new HashMap<>();
        details.put("accountLastFour", request.getAccountLastFour());
        details.put("processedAt", LocalDateTime.now().toString());

        payout.setTransactionDetails(details);

        return payoutRepository.save(payout);
    }

    // ---------------- S5-F1: SEARCH ----------------

    public List<Payout> searchPayouts(PayoutStatus status,
                                      LocalDate startDate,
                                      LocalDate endDate) {

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startDate != null) {
            startDateTime = startDate.atStartOfDay();
        }

        if (endDate != null) {
            endDateTime = endDate.atTime(23, 59, 59);
        }

        if (status == null && startDateTime == null && endDateTime == null) {
            return payoutRepository.findAll();
        }

        if (status != null && startDateTime == null && endDateTime == null) {
            return payoutRepository.findByStatusOrderByCreatedAtDesc(status);
        }

        if (status == null) {
            return payoutRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
        }

        return payoutRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                status, startDateTime, endDateTime
        );
    }

    // ---------------- S5-F6: REVENUE REPORT ----------------

    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {

        // a) Validate dates
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startDate cannot be after endDate"
            );
        }

        // Convert to LocalDateTime
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // Execute query
        List<Object[]> rows = payoutRepository.getRevenueReport(start, end);

        // ALWAYS one row because of SUM/COUNT
        Object[] result = rows.get(0);

        double totalRevenue = ((Number) result[0]).doubleValue();
        long totalTransactions = ((Number) result[1]).longValue();
        double refundedAmount = ((Number) result[2]).doubleValue();
        long refundCount = ((Number) result[3]).longValue();

        double averagePayout =
                totalTransactions == 0 ? 0 : totalRevenue / totalTransactions;

        return new RevenueReportDTO(
                totalRevenue,
                totalTransactions,
                averagePayout,
                refundedAmount,
                refundCount
        );
    }

    // ---------------- S5-F9: PROMO REPORT ----------------

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

    // ---------------- S5-F7: RETRY ----------------

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

    // ---------------- S5-F2: REFUND ----------------

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

        int retryAttempt = 0;
        Object retryValue = transactionDetails.get("retryAttempt");

        if (retryValue instanceof Number) {
            retryAttempt = ((Number) retryValue).intValue();
        } else if (retryValue instanceof String) {
            try {
                retryAttempt = Integer.parseInt((String) retryValue);
            } catch (Exception ignored) {}
        }

        transactionDetails.put("retryAttempt", retryAttempt + 1);
        transactionDetails.put("gatewayResponse", "approved");
        transactionDetails.put("refundReason", reason);
        transactionDetails.put("refundedAt", LocalDateTime.now().toString());

        payout.setTransactionDetails(transactionDetails);

        return payoutRepository.save(payout);
    }
}