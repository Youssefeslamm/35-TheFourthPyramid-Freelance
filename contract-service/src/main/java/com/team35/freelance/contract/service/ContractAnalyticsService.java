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
        Object[] row = aggregateRows == null || aggregateRows.isEmpty() ? null : aggregateRows.get(0);

        long totalContracts = numberAsLong(row, 0);
        double averageContractValue = numberAsDouble(row, 1);
        long completedContracts = numberAsLong(row, 2);
        double averageContractDurationDays = numberAsDouble(row, 3);

        double completionRate = totalContracts > 0 ? ((double) completedContracts / totalContracts) : 0.0;

        Map<String, Long> contractsByStatus = new HashMap<>();
        List<Object[]> statusRows = contractRepository.countContractsByStatus(startDate, endDate);
        for (Object[] statusRow : statusRows) {
            if (statusRow != null && statusRow.length > 1 && statusRow[0] != null && statusRow[1] != null) {
                contractsByStatus.put(String.valueOf(statusRow[0]), ((Number) statusRow[1]).longValue());
            }
        }

        ContractAnalyticsDTO analytics = ContractAnalyticsDTO.builder()
                .totalContracts(totalContracts)
                .averageContractValue(averageContractValue)
                .completionRate(completionRate)
                .averageContractDurationDays(averageContractDurationDays)
                .contractsByStatus(contractsByStatus)
                .build();

        setInCache(cacheKey, analytics);
        saveSnapshot(startDate, endDate, analytics);

        return analytics;
    }

    private ContractAnalyticsDTO getFromCache(String cacheKey) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson == null || cachedJson.isBlank()) {
                return null;
            }
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
}
