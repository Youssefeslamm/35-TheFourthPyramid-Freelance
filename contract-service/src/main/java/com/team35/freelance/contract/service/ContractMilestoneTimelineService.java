package com.team35.freelance.contract.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team35.freelance.contract.cassandra.ContractMilestoneEvent;
import com.team35.freelance.contract.dto.ContractMilestoneDTO;
import com.team35.freelance.contract.repository.ContractMilestoneEventRepository;
import com.team35.freelance.contract.repository.ContractRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ContractMilestoneTimelineService {

    private final ContractRepository contractRepository;
    private final ContractMilestoneEventRepository milestoneEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ContractMilestoneTimelineService(ContractRepository contractRepository,
                                            ContractMilestoneEventRepository milestoneEventRepository,
                                            StringRedisTemplate redisTemplate,
                                            ObjectMapper objectMapper) {
        this.contractRepository = contractRepository;
        this.milestoneEventRepository = milestoneEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ContractMilestoneDTO> getTimeline(Long contractId, LocalDateTime startTime, LocalDateTime endTime) {
        if (contractRepository.findById(contractId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found");
        }

        String cacheKey = "contract:milestones:timeline:" + contractId + ":" +
                Objects.toString(startTime, "null") + ":" + Objects.toString(endTime, "null");
        List<ContractMilestoneDTO> cached = getFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        Instant start = startTime == null ? null : startTime.atZone(ZoneId.systemDefault()).toInstant();
        Instant end = endTime == null ? null : endTime.atZone(ZoneId.systemDefault()).toInstant();

        List<ContractMilestoneDTO> timeline = milestoneEventRepository.findByKeyContractId(contractId).stream()
                .filter(event -> {
                    Instant ts = event.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant();
                    boolean afterStart = start == null || !ts.isBefore(start);
                    boolean beforeEnd = end == null || !ts.isAfter(end);
                    return afterStart && beforeEnd;
                })
                .map(this::toDto)
                // Guarantee newest-first ordering even if repository order changes.
                .sorted(Comparator.comparing(ContractMilestoneDTO::getTimestamp).reversed())
                .collect(Collectors.toList());

        setInCache(cacheKey, timeline);
        return timeline;
    }

    private ContractMilestoneDTO toDto(ContractMilestoneEvent event) {
        return new ContractMilestoneDTO(
                event.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                event.getMilestoneOrder(),
                event.getStatus(),
                event.getRecordedBy(),
                event.getNotes()
        );
    }

    private List<ContractMilestoneDTO> getFromCache(String cacheKey) {
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedJson == null || cachedJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(cachedJson, new TypeReference<List<ContractMilestoneDTO>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private void setInCache(String cacheKey, List<ContractMilestoneDTO> timeline) {
        try {
            String payload = objectMapper.writeValueAsString(timeline);
            redisTemplate.opsForValue().set(cacheKey, payload, Duration.ofMinutes(5));
        } catch (Exception ignored) {
            // Cache failures must not block timeline responses.
        }
    }
}
