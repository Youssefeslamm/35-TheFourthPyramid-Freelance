package com.team35.freelance.wallet.service;

import com.team35.freelance.wallet.common.refund.RefundRequest;
import com.team35.freelance.wallet.dto.*;
import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutStatus;
import com.team35.freelance.wallet.repository.PayoutRepository;
import com.team35.freelance.wallet.repository.PromoCodeRepository;
import com.team35.freelance.wallet.common.refund.*;
import com.team35.freelance.wallet.common.observer.EntityObserver;
import com.team35.freelance.wallet.common.observer.MongoEventLogger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team35.freelance.wallet.dto.PayoutMethodDTO;
import com.team35.freelance.wallet.common.event.PayoutAuditEvent;
import com.team35.freelance.wallet.repository.MongoEventRepository;
import java.time.LocalTime;
import java.util.stream.Collectors;

import java.util.HashMap;
import java.util.Map;

import com.team35.freelance.wallet.dto.CategoryRevenueDTO;
import java.util.List;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final RefundStrategySelector refundStrategySelector;
    private final MongoEventRepository mongoEventRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final WalletAnalyticsCacheService walletAnalyticsCacheService;

    public PayoutService(PayoutRepository payoutRepository,
                         PromoCodeRepository promoCodeRepository,
                         RefundStrategySelector refundStrategySelector,
                         MongoEventLogger mongoEventLogger,
                         MongoEventRepository mongoEventRepository,
                         WalletAnalyticsCacheService walletAnalyticsCacheService) {

        this.payoutRepository = payoutRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.refundStrategySelector = refundStrategySelector;
        this.mongoEventRepository = mongoEventRepository;
        this.walletAnalyticsCacheService = walletAnalyticsCacheService;
        registerObserver(mongoEventLogger);
    }
    public void registerObserver(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
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
    public Payout createPayout(Payout payout) {
        payout.setCreatedAt(LocalDateTime.now());
        Payout saved = payoutRepository.save(payout);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PAYOUT_CREATED");
        payload.put("payoutId", saved.getId());
        payload.put("amount", saved.getAmount());

        notifyObservers("PAYOUT_AUDIT", payload);

        return saved;    }

    public List<Payout> getAllPayouts() {
        return payoutRepository.findAll();
    }

    @Cacheable(value = "wallet-service::payout", key = "#id")
    public Payout getPayoutById(Long id) {
        return payoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payout not found"));
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
    public Payout updatePayout(Long id, Payout updatedPayout) {
        Payout existing = getPayoutById(id);

        existing.setContractId(updatedPayout.getContractId());
        existing.setFreelancerId(updatedPayout.getFreelancerId());
        existing.setAmount(updatedPayout.getAmount());
        existing.setMethod(updatedPayout.getMethod());
        existing.setStatus(updatedPayout.getStatus());
        existing.setTransactionDetails(updatedPayout.getTransactionDetails());

        Payout saved = payoutRepository.save(existing);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PAYOUT_UPDATED");
        payload.put("payoutId", saved.getId());

        notifyObservers("PAYOUT_AUDIT", payload);

        return saved;    }

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
    public void deletePayout(Long id) {
        if (!payoutRepository.existsById(id)) {
            throw new RuntimeException("Payout not found");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PAYOUT_DELETED");
        payload.put("payoutId", id);

        notifyObservers("PAYOUT_AUDIT", payload);

        payoutRepository.deleteById(id);    }

    @Cacheable(value = "wallet-service::S5-F3", key = "#freelancerId")
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
    public void processContractPayout(Long contractId, ProcessPayoutRequest request) {

        String contractStatus = payoutRepository.getContractStatus(contractId);

        if (contractStatus == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found");
        }

        if (!"COMPLETED".equals(contractStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract is not completed");
        }

        Payout payout = payoutRepository.findByContractId(contractId);

        if (payout == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found");
        }

        if (payout.getStatus() == PayoutStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "already paid");
        }

        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setMethod(request.getMethod());

        Map<String, Object> details = new HashMap<>();

        // existing fields
        if (request.getAccountLastFour() != null) {
            details.put("accountLastFour", request.getAccountLastFour());
        }

        // ✅ NEW REQUIRED FIELD (Section 4.6)
        double platformFee = payout.getAmount() * 0.10;
        details.put("platformFee", platformFee);

        payout.setTransactionDetails(details);

        Payout saved = payoutRepository.save(payout);

        // existing observer logic
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PAYOUT_PROCESSED");
        payload.put("payoutId", saved.getId());

        notifyObservers("PAYOUT_AUDIT", payload);
    }

    @Cacheable(value = "wallet-service::S5-F1", key = "#status + ':' + #startDate + ':' + #endDate")
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

    @Cacheable(value = "wallet-service::S5-F6", key = "#startDate + ':' + #endDate")
    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startDate cannot be after endDate"
            );
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Object[]> rows = payoutRepository.getRevenueReport(start, end);
        Object[] result = rows.get(0);

        double totalRevenue = ((Number) result[0]).doubleValue();
        long totalTransactions = ((Number) result[1]).longValue();
        double refundedAmount = ((Number) result[2]).doubleValue();
        long refundCount = ((Number) result[3]).longValue();

        double averagePayout =
                totalTransactions == 0 ? 0 : totalRevenue / totalTransactions;

        return RevenueReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .averagePayout(averagePayout)
                .refundedAmount(refundedAmount)
                .refundCount(refundCount)
                .build();
    }

    @Cacheable(value = "wallet-service::S5-F9", key = "#limit")
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

            PromoCodeUsage dto = PromoCodeUsage.builder()
                    .promoCodeId(promoCodeId)
                    .code(code)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .timesUsed(timesUsed)
                    .totalDiscountGiven(totalDiscountGiven)
                    .active(active)
                    .expired(expired)
                    .build();

            result.add(dto);
        }

        return result;
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
    public Payout retryFailedPayout(Long id) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));

        if (payout.getStatus() != PayoutStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only failed payouts can be retried");
        }

        payout.setStatus(PayoutStatus.COMPLETED);
        Payout saved = payoutRepository.save(payout);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PAYOUT_RETRIED");
        payload.put("payoutId", saved.getId());

        notifyObservers("PAYOUT_AUDIT", payload);

        return saved;    }

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

        Payout saved = payoutRepository.save(payout);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("action", "PAYOUT_REFUNDED");
        eventPayload.put("payoutId", payout.getId());
        eventPayload.put("amount", payout.getAmount());

        notifyObservers("PAYOUT_AUDIT", eventPayload);

        return saved;    }


    @Transactional
    @CacheEvict(value = {
            "wallet-service::S5-F10",
            "wallet-service::S5-F11",
            "wallet-service::payout"
    }, allEntries = true)
    public Payout reversePayout(Long payoutId, String reversalScope, String reason) {

        // 1. FIND
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payout not found"));

        // 2. VALIDATE
        if (payout.getStatus() != PayoutStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only completed payouts can be reversed"
            );
        }

        // 3. STRATEGY
        com.team35.freelance.wallet.common.refund.RefundRequest internalRequest =
                new com.team35.freelance.wallet.common.refund.RefundRequest(reversalScope);

        RefundStrategy strategy = refundStrategySelector.select(payout, internalRequest);
        RefundResult result = strategy.calculateRefund(payout, internalRequest);

        String strategyName = strategy.getClass().getSimpleName();

        // 4. DENIED CASE
        if (strategy instanceof NoReversalStrategy) {

            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "REFUND_DENIED");
            payload.put("payoutId", payout.getId());
            payload.put("strategy", strategyName);
            payload.put("reason", "reversal window expired");

            notifyObservers("PAYOUT_AUDIT", payload);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "reversal window expired"
            );
        }

        // 5. SUCCESS
        payout.setStatus(PayoutStatus.REFUNDED);

        Map<String, Object> details = payout.getTransactionDetails();
        if (details == null) details = new HashMap<>();

        details.put("refundAmount", result.getAmount());
        details.put("reversalScope", reversalScope);
        details.put("refundReason", reason);
        details.put("refundedAt", LocalDateTime.now().toString());

        payout.setTransactionDetails(details);

        Payout saved = payoutRepository.save(payout);

        // 6. AUDIT EVENT (FULL REQUIRED DATA)
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "REFUNDED");
        payload.put("payoutId", payout.getId());
        payload.put("originalAmount", payout.getAmount());
        payload.put("refundAmount", result.getAmount());
        payload.put("reversalScope", reversalScope);
        payload.put("strategy", strategyName);
        payload.put("reason", reason);

        notifyObservers("PAYOUT_AUDIT", payload);

        return saved;
    }

    public List<CategoryRevenueDTO> getCategoryRevenueAnalytics(LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        // ✅ ALWAYS runs
        logAnalyticsViewedEvent(startDate, endDate);

        // ✅ cached logic
        return walletAnalyticsCacheService.getCategoryRevenueAnalyticsCached(startDate, endDate);
    }
    private void logAnalyticsViewedEvent(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "ANALYTICS_VIEWED");
        payload.put("feature", "S5-F10");
        payload.put("startDate", startDate.toString());
        payload.put("endDate", endDate.toString());

        notifyObservers("PAYOUT_AUDIT", payload);
    }


    // S5-F11
    @Cacheable(value = "wallet-service::S5-F11", key = "#startDate + ':' + #endDate")
    public List<PayoutMethodDTO> getPayoutMethodBreakdown(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.of(23, 59, 59, 999_000_000));

        List<PayoutAuditEvent> events = mongoEventRepository
                .findByActionInAndTimestampBetween(List.of("COMPLETED", "FAILED"), start, end);

        Map<String, List<PayoutAuditEvent>> byMethod = events.stream()
                .filter(e -> e.getMethod() != null && !e.getMethod().isBlank())
                .collect(Collectors.groupingBy(PayoutAuditEvent::getMethod));

        List<PayoutMethodDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<PayoutAuditEvent>> entry : byMethod.entrySet()) {
            String method = entry.getKey();
            List<PayoutAuditEvent> group = entry.getValue();
            long successCount = group.stream().filter(e -> "COMPLETED".equals(e.getAction())).count();
            long failureCount = group.stream().filter(e -> "FAILED".equals(e.getAction())).count();
            long denominator = successCount + failureCount;
            double successRate = denominator == 0 ? 0.0 : (double) successCount / denominator;
            double totalAmount = group.stream()
                    .filter(e -> "COMPLETED".equals(e.getAction()) && e.getAmount() != null)
                    .mapToDouble(PayoutAuditEvent::getAmount).sum();
            result.add(PayoutMethodDTO.builder()
                    .method(method).successCount(successCount).failureCount(failureCount)
                    .successRate(successRate).totalAmount(totalAmount).build());
        }
        return result;
    }
}