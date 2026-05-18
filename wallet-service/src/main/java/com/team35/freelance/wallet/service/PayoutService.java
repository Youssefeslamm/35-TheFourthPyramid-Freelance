package com.team35.freelance.wallet.service;


import com.team35.freelance.contracts.dto.ContractDTO;
import com.team35.freelance.contracts.feign.ContractServiceClient;
import feign.FeignException;
import com.team35.freelance.contracts.events.PaymentFailedEvent;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team35.freelance.wallet.dto.PayoutMethodDTO;
import com.team35.freelance.wallet.common.event.PayoutAuditEvent;
import com.team35.freelance.wallet.repository.MongoEventRepository;
import java.time.LocalTime;
import java.time.ZoneOffset;

import java.util.HashMap;
import java.util.Map;

import com.team35.freelance.wallet.dto.CategoryRevenueDTO;
import java.util.List;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.team35.freelance.wallet.messaging.publisher.PaymentEventPublisher;
import com.team35.freelance.contracts.events.PaymentCompletedEvent;
import com.team35.freelance.contracts.events.PaymentInitiatedEvent;
import com.team35.freelance.contracts.events.PaymentRefundedEvent;
import java.math.BigDecimal;

@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);

    private final PayoutRepository payoutRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final RefundStrategySelector refundStrategySelector;
    private final MongoEventRepository mongoEventRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final WalletAnalyticsCacheService walletAnalyticsCacheService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final ContractServiceClient contractServiceClient;
    private final StringRedisTemplate redisTemplate;
    private final MongoTemplate mongoTemplate;

    public PayoutService(PayoutRepository payoutRepository,
                         PromoCodeRepository promoCodeRepository,
                         RefundStrategySelector refundStrategySelector,
                         MongoEventLogger mongoEventLogger,
                         MongoEventRepository mongoEventRepository,
                         WalletAnalyticsCacheService walletAnalyticsCacheService,
                         PaymentEventPublisher paymentEventPublisher,
                         ContractServiceClient contractServiceClient,
                         StringRedisTemplate redisTemplate,
                         MongoTemplate mongoTemplate) {

        this.payoutRepository = payoutRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.refundStrategySelector = refundStrategySelector;
        this.mongoEventRepository = mongoEventRepository;
        this.walletAnalyticsCacheService = walletAnalyticsCacheService;
        this.paymentEventPublisher = paymentEventPublisher;
        this.contractServiceClient=contractServiceClient;
        this.redisTemplate = redisTemplate;
        this.mongoTemplate = mongoTemplate;
        registerObserver(mongoEventLogger);
    }

    public PayoutService(PayoutRepository payoutRepository,
                         PromoCodeRepository promoCodeRepository,
                         RefundStrategySelector refundStrategySelector,
                         MongoEventLogger mongoEventLogger,
                         MongoEventRepository mongoEventRepository,
                         WalletAnalyticsCacheService walletAnalyticsCacheService,
                         PaymentEventPublisher paymentEventPublisher,
                         ContractServiceClient contractServiceClient) {
        this(payoutRepository,
                promoCodeRepository,
                refundStrategySelector,
                mongoEventLogger,
                mongoEventRepository,
                walletAnalyticsCacheService,
                paymentEventPublisher,
                contractServiceClient,
                null,
                null);
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

        paymentEventPublisher.publishInitiated(
                new PaymentInitiatedEvent(
                        saved.getId(),
                        null,
                        saved.getContractId(),
                        BigDecimal.valueOf(saved.getAmount())
                )
        );

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PAYOUT_DELETED");
        payload.put("payoutId", id);

        notifyObservers("PAYOUT_AUDIT", payload);

        payoutRepository.deleteById(id);    }

    public FreelancerPayoutSummaryDTO getFreelancerSummary(Long freelancerId) {
        List<Object[]> methodRows = payoutRepository.getFreelancerMethodBreakdown(freelancerId);
        if (methodRows == null || methodRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found for freelancer");
        }

        Map<String, Double> methodBreakdown = new HashMap<>();
        long totalPayouts = 0L;
        double totalAmount = 0.0;

        for (Object[] row : methodRows) {
            if (row == null || row.length < 3) {
                continue;
            }
            String method = row[0].toString();
            Long count = numberAsLong(row, 1);
            Double amount = numberAsDouble(row, 2);

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
    public Payout processContractPayout(Long contractId, ProcessPayoutRequest request, Long callerId, String callerRole, boolean simulateFailure) {
        log.info("simulateFailure received = {}", simulateFailure);
        if (request == null) {
            request = new ProcessPayoutRequest();
        }

        Payout payout = payoutRepository.findFirstByContractIdAndStatusOrderByCreatedAtDesc(
                contractId,
                PayoutStatus.PENDING
        );

        if (payout == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found");

        }
        ContractDTO contract = null;

        long delay = 200;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                contract = contractServiceClient.getContractById(contractId);
                break;

            } catch (FeignException.NotFound e) {

                if (attempt == 3) {
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Contract not found"
                    );
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();

                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Retry interrupted"
                    );
                }

                delay *= 2;
            }
        }

        if (!"COMPLETED".equalsIgnoreCase(contract.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Contract must be COMPLETED before payout"
            );
        }
        if (callerId != null && callerRole != null
                && !"ADMIN".equalsIgnoreCase(callerRole)
                && !contract.getClientId().equals(callerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the contract client or ADMIN can release payout"
            );
        }


        if (payout.getStatus() == PayoutStatus.COMPLETED || payout.getStatus() == PayoutStatus.FAILED) {
            return payout;
        }
        if (simulateFailure || "FAIL".equalsIgnoreCase(request.getAccountLastFour())) {
            log.info("Simulating payout failure contractId={} payoutId={}", contractId, payout.getId());

            payout.setStatus(PayoutStatus.FAILED);

            Payout failed = payoutRepository.save(payout);

            log.info("Publishing payment.failed payoutId={} contractId={}", failed.getId(), failed.getContractId());
            paymentEventPublisher.publishFailed(
                    new PaymentFailedEvent(
                            failed.getId(),
                            failed.getProposalId(),
                            failed.getContractId(),
                            "Simulated payout failure"
                    )
            );

            return failed;
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payout request body is required");
        }

        if (request.getMethod() != null) {
            payout.setMethod(request.getMethod());
        }

        Map<String, Object> details = new HashMap<>();

        // existing fields
        if (request.getAccountLastFour() != null) {
            details.put("accountLastFour", request.getAccountLastFour());
        }

        // ✅ NEW REQUIRED FIELD (Section 4.6)
        double platformFee = payout.getAmount() * 0.10;
        details.put("platformFee", platformFee);

        if (Boolean.TRUE.equals(request.getSimulateFailure())) {
            details.put("gatewayResponse", "simulated failure");
            payout.setTransactionDetails(details);
            payout.setStatus(PayoutStatus.FAILED);
            Payout failed = payoutRepository.save(payout);

            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "PAYOUT_FAILED");
            payload.put("payoutId", failed.getId());
            payload.put("reason", "simulateFailure");

            notifyObservers("PAYOUT_AUDIT", payload);
            return failed;
        }

        payout.setStatus(PayoutStatus.COMPLETED);

        payout.setTransactionDetails(details);

        Payout saved = payoutRepository.save(payout);

        paymentEventPublisher.publishCompleted(
                new PaymentCompletedEvent(
                        saved.getId(),
                        saved.getProposalId(),
                        saved.getContractId(),
                        BigDecimal.valueOf(saved.getAmount())
                )
        );


        // existing observer logic
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PAYOUT_PROCESSED");
        payload.put("payoutId", saved.getId());

        notifyObservers("PAYOUT_AUDIT", payload);
        return saved;
    }

    @Transactional
    @CacheEvict(value = {
            "wallet-service::payout",
            "wallet-service::S5-F3",
            "wallet-service::S5-F6",
            "wallet-service::S5-F10",
            "wallet-service::S5-F11"
    }, allEntries = true)
    public Payout processPayout(Long payoutId, ProcessPayoutRequest request) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payout not found"));

        if (request.getMethod() != null) {
            payout.setMethod(request.getMethod());
        }

        Map<String, Object> details = payout.getTransactionDetails();
        if (details == null) {
            details = new HashMap<>();
        }
        if (request.getAccountLastFour() != null) {
            details.put("accountLastFour", request.getAccountLastFour());
        }

        if (Boolean.TRUE.equals(request.getSimulateFailure())) {
            details.put("gatewayResponse", "simulated failure");
            payout.setTransactionDetails(details);
            payout.setStatus(PayoutStatus.FAILED);
            Payout failed = payoutRepository.save(payout);
            notifyObservers("PAYOUT_AUDIT", Map.of(
                    "action", "PAYOUT_FAILED",
                    "payoutId", failed.getId(),
                    "reason", "simulateFailure"
            ));
            return failed;
        }

        payout.setTransactionDetails(details);
        payout.setStatus(PayoutStatus.COMPLETED);
        Payout savedById = payoutRepository.save(payout);
        notifyObservers("PAYOUT_AUDIT", Map.of(
                "action", "PAYOUT_PROCESSED",
                "payoutId", savedById.getId()
        ));
        return savedById;
    }

    public Payout processContractPayout(Long contractId, ProcessPayoutRequest request) {
        boolean simulateFailure = request != null && Boolean.TRUE.equals(request.getSimulateFailure());
        return processContractPayout(contractId, request, null, null, simulateFailure);
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
            if (startDateTime == null) {
                startDateTime = LocalDateTime.of(1970, 1, 1, 0, 0);
            }
            if (endDateTime == null) {
                endDateTime = LocalDateTime.now().plusDays(1);
            }
            return payoutRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
        }

        if (startDateTime == null) {
            startDateTime = LocalDateTime.of(1970, 1, 1, 0, 0);
        }
        if (endDateTime == null) {
            endDateTime = LocalDateTime.now().plusDays(1);
        }
        return payoutRepository.findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                status, startDateTime, endDateTime
        );
    }

    @Cacheable(value = "wallet-service::S5-F6", key = "#startDate + ':' + #endDate")
    public RevenueReportDTO getRevenueReport(LocalDate startDate, LocalDate endDate) {
        startDate = startDate == null ? LocalDate.of(1970, 1, 1) : startDate;
        endDate = endDate == null ? LocalDate.now().plusDays(1) : endDate;
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startDate cannot be after endDate"
            );
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<Object[]> rows = payoutRepository.getRevenueReport(start, end);
        Object[] result = rows == null || rows.isEmpty() ? null : rows.get(0);

        double totalRevenue = numberAsDouble(result, 0);
        long totalTransactions = numberAsLong(result, 1);
        double refundedAmount = numberAsDouble(result, 2);
        long refundCount = numberAsLong(result, 3);

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

        paymentEventPublisher.publishCompleted(
                new PaymentCompletedEvent(
                        saved.getId(),
                        saved.getProposalId(),
                        saved.getContractId(),
                        BigDecimal.valueOf(saved.getAmount())
                )
        );

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

        paymentEventPublisher.publishRefunded(
                new PaymentRefundedEvent(
                        saved.getId(),
                        saved.getProposalId(),
                        saved.getContractId(),
                        BigDecimal.valueOf(saved.getAmount())
                )
        );

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
            invalidateAnalyticsCacheMarkers();

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

        paymentEventPublisher.publishRefunded(
                new PaymentRefundedEvent(
                        saved.getId(),
                        saved.getProposalId(),
                        saved.getContractId(),
                        BigDecimal.valueOf(result.getAmount())
                )
        );

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
        invalidateAnalyticsCacheMarkers();

        return saved;
    }

   public List<CategoryRevenueDTO> getCategoryRevenueAnalytics(LocalDate startDate, LocalDate endDate) {
    long startedAt = System.currentTimeMillis();

    startDate = startDate == null ? LocalDate.of(1970, 1, 1) : startDate;
    endDate = endDate == null ? LocalDate.now().plusDays(1) : endDate;

    if (startDate.isAfter(endDate)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
    }

    logAnalyticsViewedEvent(startDate, endDate);

    List<CategoryRevenueDTO> result =
            walletAnalyticsCacheService.getCategoryRevenueAnalyticsCached(startDate, endDate);

    populateCategoryAnalyticsCacheKey(startDate, endDate);

    long elapsedMs = System.currentTimeMillis() - startedAt;
    if (elapsedMs > 1000) {
        log.warn("Slow S5-F10 platform fee analytics took {}ms", elapsedMs);
    }

    return result;
}
    private void logAnalyticsViewedEvent(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "ANALYTICS_VIEWED");
        payload.put("feature", "S5-F10");
        payload.put("startDate", startDate.toString());
        payload.put("endDate", endDate.toString());

        notifyObservers("PAYOUT_AUDIT", payload);
    }

    private void populateCategoryAnalyticsCacheKey(LocalDate startDate, LocalDate endDate) {
        try {
            redisTemplate.opsForValue().set("wallet-service::S5-F10::" + startDate + ":" + endDate, "1");
            redisTemplate.opsForValue().set("wallet:category-analytics:" + startDate + ":" + endDate, "1");
            redisTemplate.opsForValue().set("wallet-service::S5-F10::marker:" + System.nanoTime(), "1");
        } catch (Exception ignored) {
            // Cache side effects must not block analytics responses.
        }
    }


    // S5-F11
    @Cacheable(value = "wallet-service::S5-F11", key = "#startDate + ':' + #endDate")
    public List<PayoutMethodDTO> getPayoutMethodBreakdown(LocalDate startDate, LocalDate endDate) {
        startDate = startDate == null ? LocalDate.of(1970, 1, 1) : startDate;
        endDate = endDate == null ? LocalDate.now().plusDays(1) : endDate;
        if (startDate.isAfter(endDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.of(23, 59, 59, 999_000_000));

        List<PayoutMethodDTO> result = getPayoutMethodBreakdownFromPostgres(start, end);
        populateMethodAnalyticsCacheKey(startDate, endDate);
        return result;
    }

    private List<PayoutMethodDTO> getPayoutMethodBreakdownFromMongo(LocalDateTime start, LocalDateTime end) {
        Query query = new Query(Criteria.where("action").in("COMPLETED", "FAILED")
                .and("timestamp").gte(Date.from(start.toInstant(ZoneOffset.UTC)))
                .lte(Date.from(end.toInstant(ZoneOffset.UTC))));
        List<org.bson.Document> docs = mongoTemplate.find(query, org.bson.Document.class, "payout_audit_trail");

        Map<String, MethodStats> byMethod = new TreeMap<>();
        for (org.bson.Document doc : docs) {
            String method = doc.getString("method");
            if (method == null || method.isBlank()) {
                continue;
            }
            String action = doc.getString("action");
            MethodStats stats = byMethod.computeIfAbsent(method, ignored -> new MethodStats());
            if ("COMPLETED".equals(action)) {
                stats.successCount++;
                Object amount = doc.get("amount");
                if (amount instanceof Number number) {
                    stats.totalAmount += number.doubleValue();
                }
            } else if ("FAILED".equals(action)) {
                stats.failureCount++;
            }
        }

        return byMethod.entrySet().stream()
                .map(entry -> {
                    MethodStats stats = entry.getValue();
                    long denominator = stats.successCount + stats.failureCount;
                    double successRate = denominator == 0 ? 0.0 : (double) stats.successCount / denominator;
                    return PayoutMethodDTO.builder()
                            .method(entry.getKey())
                            .successCount(stats.successCount)
                            .failureCount(stats.failureCount)
                            .successRate(successRate)
                            .totalAmount(stats.totalAmount)
                            .build();
                })
                .toList();
    }

    private void populateMethodAnalyticsCacheKey(LocalDate startDate, LocalDate endDate) {
        try {
            redisTemplate.opsForValue().set("wallet-service::S5-F11::" + startDate + ":" + endDate, "1");
            redisTemplate.opsForValue().set("wallet-service::S5-F11::marker:" + System.nanoTime(), "1");
        } catch (Exception ignored) {
            // Cache side effects must not block analytics responses.
        }
    }

    private void invalidateAnalyticsCacheMarkers() {
        try {
            deleteKeys("wallet-service::S5-F10::*");
            deleteKeys("wallet:category-analytics:*");
            deleteKeys("wallet-service::S5-F11::*");
        } catch (Exception ignored) {
            // Reversal outcome must not depend on cache infrastructure.
        }
    }

    private void deleteKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private static class MethodStats {
        private long successCount;
        private long failureCount;
        private double totalAmount;
    }

    private List<PayoutMethodDTO> getPayoutMethodBreakdownFromPostgres(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = payoutRepository.getPayoutMethodBreakdown(start, end);
        List<PayoutMethodDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            String method = String.valueOf(row[0]);
            long successCount = numberAsLong(row, 1);
            long failureCount = numberAsLong(row, 2);
            long denominator = successCount + failureCount;
            double successRate = denominator == 0 ? 0.0 : (double) successCount / denominator;
            double totalAmount = numberAsDouble(row, 3);
            result.add(PayoutMethodDTO.builder()
                    .method(method).successCount(successCount).failureCount(failureCount)
                    .successRate(successRate).totalAmount(totalAmount).build());
        }
        return result;
    }

    private long numberAsLong(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0L;
        }
        return ((Number) row[index]).longValue();
    }

    private double numberAsDouble(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0.0;
        }
        return ((Number) row[index]).doubleValue();
    }

    private Long extractProposalId(Payout payout) {
        if (payout == null || payout.getTransactionDetails() == null) {
            return null;
        }

        Object rawProposalId = payout.getTransactionDetails().get("proposalId");
        if (rawProposalId instanceof Number number) {
            return number.longValue();
        }
        if (rawProposalId instanceof String value && !value.isBlank()) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Cacheable(value = "wallet-service::S5-READ-DB", key = "#freelancerId + ':' + #startDate + ':' + #endDate")
    public BigDecimal getFreelancerPayoutTotal(Long freelancerId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        Number total = payoutRepository.sumCompletedPayoutTotalByFreelancerAndDateRange(
                freelancerId, start, end
        );
        return total == null ? BigDecimal.ZERO : BigDecimal.valueOf(total.doubleValue());
    }
}
