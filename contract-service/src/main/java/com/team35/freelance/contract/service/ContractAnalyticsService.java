package com.team35.freelance.contract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team35.freelance.contract.document.ContractAnalyticsSnapshot;
import com.team35.freelance.contract.dto.ContractAnalyticsDTO;
import com.team35.freelance.contract.repository.ContractAnalyticsSnapshotRepository;
import com.team35.freelance.contract.repository.ContractRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContractAnalyticsService {

    private final ContractRepository contractRepository;
    private final ContractAnalyticsSnapshotRepository snapshotRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ContractAnalyticsService(ContractRepository contractRepository,
                                    ContractAnalyticsSnapshotRepository snapshotRepository,
                                    StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.contractRepository = contractRepository;
        this.snapshotRepository = snapshotRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public ContractAnalyticsDTO getContractAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        String cacheKey = buildCacheKey(startDate, endDate);
        ContractAnalyticsDTO cached = getFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Object[]> aggregateRows = contractRepository.getContractAnalytics(startDate, endDate);
        Object[] row = aggregateRows.get(0);

        long totalContracts = ((Number) row[0]).longValue();
        double averageContractValue = ((Number) row[1]).doubleValue();
        long completedContracts = ((Number) row[2]).longValue();
        double averageContractDurationDays = ((Number) row[3]).doubleValue();

        double completionRate = totalContracts > 0 ? ((double) completedContracts / totalContracts) : 0.0;

        Map<String, Long> contractsByStatus = new HashMap<>();
        List<Object[]> statusRows = contractRepository.countContractsByStatus(startDate, endDate);
        for (Object[] statusRow : statusRows) {
            contractsByStatus.put((String) statusRow[0], ((Number) statusRow[1]).longValue());
        }

        ContractAnalyticsDTO analytics = new ContractAnalyticsDTO(
                totalContracts,
                averageContractValue,
                completionRate,
                averageContractDurationDays,
                contractsByStatus
        );

        setInCache(cacheKey, analytics);
        saveSnapshot(startDate, endDate, analytics);

        return analytics;
    }

    private ContractAnalyticsDTO getFromCache(String cacheKey) {
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedJson == null || cachedJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(cachedJson, ContractAnalyticsDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void setInCache(String cacheKey, ContractAnalyticsDTO analytics) {
        try {
            String payload = objectMapper.writeValueAsString(analytics);
            redisTemplate.opsForValue().set(cacheKey, payload, Duration.ofMinutes(10));
        } catch (Exception ignored) {
            // Cache failures must not block analytics responses.
        }
    }

    private void saveSnapshot(LocalDateTime startDate, LocalDateTime endDate, ContractAnalyticsDTO analytics) {
        try {
            ContractAnalyticsSnapshot snapshot = new ContractAnalyticsSnapshot();
            snapshot.setStartDate(startDate);
            snapshot.setEndDate(endDate);
            snapshot.setGeneratedAt(LocalDateTime.now());
            snapshot.setAnalytics(analytics);
            snapshotRepository.save(snapshot);
        } catch (Exception ignored) {
            // Snapshot persistence should not break endpoint responses.
        }
    }

    private String buildCacheKey(LocalDateTime startDate, LocalDateTime endDate) {
        return "contract:analytics:" + startDate + ":" + endDate;
    }
}
