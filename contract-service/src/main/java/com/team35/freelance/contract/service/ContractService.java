package com.team35.freelance.contract.service;

import com.team35.freelance.contract.cassandra.ContractMilestoneEvent;
import com.team35.freelance.contract.cassandra.ContractMilestoneEventRepository;
import com.team35.freelance.contract.dto.BatchStatusUpdateDTO;
import com.team35.freelance.contract.dto.ContractSummaryDTO;
import com.team35.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team35.freelance.contract.dto.ContractAnalyticsDTO;
import com.team35.freelance.contract.dto.MilestoneTrackRequestDTO;
import com.team35.freelance.contract.dto.StalledContractDTO;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.ContractStatus;
import com.team35.freelance.contract.repository.ContractAnalyticsRepository;
import com.team35.freelance.contract.repository.ContractRepository;
import com.team35.freelance.contract.common.observer.EntityObserver;
import com.team35.freelance.contract.common.observer.MongoEventLogger;
import com.team35.freelance.contract.messaging.publisher.ContractEventPublisher;
import com.team35.freelance.contracts.events.ContractCreatedEvent;
import com.team35.freelance.contracts.events.ContractStatusChangedEvent;
import java.math.BigDecimal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractAnalyticsRepository analyticsRepository;
    private final ContractMilestoneEventRepository milestoneEventRepository;
    private final ContractEventPublisher contractEventPublisher;
    private final List<EntityObserver> observers = new ArrayList<>();

    private static final java.util.Set<String> ALLOWED_MILESTONE_STATUSES =
            java.util.Set.of("PENDING", "IN_PROGRESS", "COMPLETED", "APPROVED");

    public ContractService(ContractRepository contractRepository,
                           ContractAnalyticsRepository analyticsRepository,
                           ContractMilestoneEventRepository milestoneEventRepository,
                           MongoEventLogger mongoEventLogger,
                           ContractEventPublisher contractEventPublisher) {
        this.contractRepository = contractRepository;
        this.analyticsRepository = analyticsRepository;
        this.milestoneEventRepository = milestoneEventRepository;
        this.contractEventPublisher = contractEventPublisher;
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
            "contract-service::contract",
            "contract-service::S4-F1",
            "contract-service::S4-F3",
            "contract-service::S4-F5",
            "contract-service::S4-F6",
            "contract-service::S4-F8",
            "contract-service::S4-F9"
    }, allEntries = true)
    public Contract create(Contract contract) {
        Contract saved = contractRepository.save(contract);

        contractEventPublisher.publishCreated(
                new ContractCreatedEvent(
                        saved.getId(),
                        saved.getProposalId(),
                        saved.getJobId(),
                        saved.getFreelancerId(),
                        BigDecimal.valueOf(saved.getAgreedAmount())
                )
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "CREATED");
        payload.put("contractId", saved.getId());

        notifyObservers("CONTRACT", payload);

        return saved;
    }

    public List<Contract> findAll() {
        return contractRepository.findAll();
    }

    @Cacheable(value = "contract-service::contract", key = "#id")
    public Optional<Contract> findById(Long id) {

        return contractRepository.findById(id);
    }

    @CacheEvict(value = {
            "contract-service::contract",
            "contract-service::S4-F1",
            "contract-service::S4-F3",
            "contract-service::S4-F5",
            "contract-service::S4-F6",
            "contract-service::S4-F8",
            "contract-service::S4-F9"
    }, allEntries = true)
    public Contract update(Long id, Contract updatedContract) {
        Contract existing = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        ContractStatus oldStatus = existing.getStatus();

        existing.setFreelancerId(updatedContract.getFreelancerId());
        existing.setClientId(updatedContract.getClientId());
        existing.setAgreedAmount(updatedContract.getAgreedAmount());
        existing.setStatus(updatedContract.getStatus());
        existing.setStartDate(updatedContract.getStartDate());
        existing.setEndDate(updatedContract.getEndDate());
        existing.setMetadata(updatedContract.getMetadata());

        Contract saved = contractRepository.save(existing);

        if (oldStatus != null && saved.getStatus() != null && oldStatus != saved.getStatus()) {
            contractEventPublisher.publishStatusChanged(
                    new ContractStatusChangedEvent(saved.getId(), oldStatus.name(), saved.getStatus().name())
            );
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "UPDATED");
        payload.put("contractId", saved.getId());

        notifyObservers("CONTRACT", payload);

        return saved;
    }

    @CacheEvict(value = {
            "contract-service::contract",
            "contract-service::S4-F1",
            "contract-service::S4-F3",
            "contract-service::S4-F5",
            "contract-service::S4-F6",
            "contract-service::S4-F8",
            "contract-service::S4-F9"
    }, allEntries = true)
    public void delete(Long id) {
        if (!contractRepository.existsById(id)) {
            throw new RuntimeException("Contract not found");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "DELETED");
        payload.put("contractId", id);

        notifyObservers("CONTRACT", payload);

        contractRepository.deleteById(id);
    }

    @Cacheable(value = "contract-service::S4-F1", key = "#userId")
    public Contract getActiveContractForUser(Long userId) {

        return contractRepository.findMostRecentActiveContractByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No active contract found for this user"));
    }

    @CacheEvict(value = {
            "contract-service::contract",
            "contract-service::S4-F1",
            "contract-service::S4-F3",
            "contract-service::S4-F5",
            "contract-service::S4-F6",
            "contract-service::S4-F8",
            "contract-service::S4-F9"
    }, allEntries = true)
    public Contract updateProgress(Long contractId, Map<String, Object> updates) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        Map<String, Object> metadata = contract.getMetadata();

        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }

        metadata.putAll(updates);
        contract.setMetadata(metadata);

        Contract saved = contractRepository.save(contract);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "PROGRESS_UPDATED");
        payload.put("contractId", saved.getId());
        payload.put("updates", updates);

        notifyObservers("CONTRACT", payload);

        return saved;
    }

    @Cacheable(value = "contract-service::S4-F6", key = "#startDate + ':' + #endDate + ':' + #status")
    public List<Contract> getContractsInDateRange(LocalDateTime startDate,
                                                  LocalDateTime endDate,
                                                  ContractStatus status) {
        String statusFilter = (status == null) ? null : status.name();
        return contractRepository.findContractsInDateRange(startDate, endDate, statusFilter);
    }

    @Cacheable(value = "contract-service::S4-F3", key = "#minAmount + ':' + #maxAmount + ':' + #status")
    public List<ContractSummaryDTO> searchByBudgetRange(Double minAmount, Double maxAmount, String status) {
        double effectiveMin = minAmount == null ? 0.0 : minAmount;
        double effectiveMax = maxAmount == null ? 1_000_000_000_000.0 : maxAmount;

        if (effectiveMin > effectiveMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minAmount must be less than or equal to maxAmount");
        }

        String statusFilter = (status == null || status.isBlank())
                ? null
                : status.trim().toUpperCase();

        List<Object[]> rows = contractRepository.searchContractsByBudgetRange(effectiveMin, effectiveMax, statusFilter);

        return rows.stream()
                .map(this::toContractSummaryDTO)
                .collect(Collectors.toList());
    }

    private ContractSummaryDTO toContractSummaryDTO(Object[] row) {
        return ContractSummaryDTO.builder()
                .contractId(((Number) row[0]).longValue())
                .freelancerName((String) row[1])
                .jobTitle((String) row[2])
                .agreedAmount(((Number) row[3]).doubleValue())
                .status(String.valueOf(row[4]))
                .durationDays(((Number) row[5]).doubleValue())
                .build();
    }

    @Transactional
    @CacheEvict(value = {
            "contract-service::contract",
            "contract-service::S4-F1",
            "contract-service::S4-F3",
            "contract-service::S4-F5",
            "contract-service::S4-F6",
            "contract-service::S4-F8",
            "contract-service::S4-F9"
    }, allEntries = true)
    public int batchUpdateStatus(List<BatchStatusUpdateDTO> updates) {
        if (updates == null || updates.isEmpty()) {
            return 0;
        }

        List<Long> ids = updates.stream()
                .map(BatchStatusUpdateDTO::getContractId)
                .collect(Collectors.toList());

        List<Contract> contracts = contractRepository.findAllById(ids);

        if (contracts.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more contracts not found");
        }

        Map<Long, ContractStatus> oldStatusById = new HashMap<>();
        for (Contract contract : contracts) {
            oldStatusById.put(contract.getId(), contract.getStatus());
        }

        for (BatchStatusUpdateDTO update : updates) {
            Contract contract = contracts.stream()
                    .filter(c -> c.getId().equals(update.getContractId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            ContractStatus newStatus;

            try {
                newStatus = ContractStatus.valueOf(update.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + update.getStatus());
            }

            if (contract.getStatus() != ContractStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contract is not ACTIVE");
            }

            if (newStatus == ContractStatus.COMPLETED && contract.getEndDate() == null) {
                contract.setEndDate(LocalDateTime.now());
            }

            contract.setStatus(newStatus);
        }

        contractRepository.saveAll(contracts);

        for (Contract contract : contracts) {
            ContractStatus oldStatus = oldStatusById.get(contract.getId());
            if (oldStatus != null && contract.getStatus() != null && oldStatus != contract.getStatus()) {
                contractEventPublisher.publishStatusChanged(
                        new ContractStatusChangedEvent(contract.getId(), oldStatus.name(), contract.getStatus().name())
                );
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "BATCH_STATUS_UPDATED");
        payload.put("count", contracts.size());
        payload.put("contractIds", ids);

        notifyObservers("CONTRACT", payload);
        return contracts.size();
    }

    @Transactional
    @CacheEvict(value = {
            "contract-service::contract",
            "contract-service::S4-F1",
            "contract-service::S4-F3",
            "contract-service::S4-F5",
            "contract-service::S4-F6",
            "contract-service::S4-F8",
            "contract-service::S4-F9"
    }, allEntries = true)
    public int purgeOldContracts(int olderThanDays) {
        int deletedCount = contractRepository.purgeOldContracts(LocalDateTime.now().minusDays(olderThanDays));

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "OLD_DATA_PURGED");
        payload.put("deletedCount", deletedCount);
        payload.put("olderThanDays", olderThanDays);

        notifyObservers("CONTRACT", payload);

        return deletedCount;
    }

    @Cacheable(value = "contract-service::S4-F8", key = "#freelancerId + ':' + #startDate + ':' + #endDate")
    public FreelancerPerformanceDTO getFreelancerPerformance(Long freelancerId,
                                                             LocalDateTime startDate,
                                                             LocalDateTime endDate) {
        List<Object[]> results = contractRepository.getFreelancerPerformanceAggregates(freelancerId, startDate, endDate);
        Object[] row = results == null || results.isEmpty() ? null : results.get(0);

        Integer totalContracts = numberAsInt(row, 0);
        Integer completedContracts = numberAsInt(row, 1);
        Double totalAmount = numberAsDouble(row, 2);
        Double totalEarnings = numberAsDouble(row, 3);
        Double avgDuration = numberAsDouble(row, 4);

        Double averageContractValue = totalContracts > 0 ? totalAmount / totalContracts : 0.0;
        Double completionRate = totalContracts > 0 ? ((double) completedContracts / totalContracts) * 100 : 0.0;

        return FreelancerPerformanceDTO.builder()
                .freelancerId(freelancerId)
                .totalContracts(totalContracts)
                .averageContractValue(averageContractValue)
                .completionRate(completionRate)
                .averageDurationDays(avgDuration)
                .totalEarnings(totalEarnings)
                .build();
    }

    @Cacheable(value = "contract-service::S4-F9", key = "#maxProgress + ':' + #stalledDays")
    public List<StalledContractDTO> getStalledContracts(Double maxProgress, Integer stalledDays) {
        List<Object[]> results = contractRepository.findStalledContracts(maxProgress, stalledDays);

        return results.stream()
                .map(row -> StalledContractDTO.builder()
                        .contractId(((Number) row[0]).longValue())
                        .freelancerName((String) row[1])
                        .jobTitle((String) row[2])
                        .agreedAmount(((Number) row[3]).doubleValue())
                        .progressPercentage(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0)
                        .daysSinceLastActivity(((Number) row[5]).intValue())
                        .build()
                )
                .collect(Collectors.toList());
    }

    private Integer numberAsInt(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0;
        }
        return ((Number) row[index]).intValue();
    }

    private Double numberAsDouble(Object[] row, int index) {
        if (row == null || row.length <= index || row[index] == null) {
            return 0.0;
        }
        return ((Number) row[index]).doubleValue();
    }

    @Cacheable(value = "contract-service::S4-F5", key = "#key + ':' + #operator + ':' + #value")
    public List<Contract> searchContractsByMetadata(String key, String operator, String value) {
        if ("eq".equalsIgnoreCase(operator)) {
            return contractRepository.findByMetadataEq(key, value);
        } else if ("gt".equalsIgnoreCase(operator)) {
            return contractRepository.findByMetadataGt(key, Double.parseDouble(value));
        } else if ("lt".equalsIgnoreCase(operator)) {
            return contractRepository.findByMetadataLt(key, Double.parseDouble(value));
        } else {
            throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
    @CacheEvict(value = {
            "contract-service::S4-F10",
            "contract-service::S4-F12"
    }, allEntries = true)
    public ContractMilestoneEvent recordMilestoneEvent(Long contractId, MilestoneTrackRequestDTO request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));

        if (request == null || request.getStatus() == null
                || !ALLOWED_MILESTONE_STATUSES.contains(request.getStatus().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be one of PENDING, IN_PROGRESS, COMPLETED, APPROVED");
        }

        ContractMilestoneEvent event = new ContractMilestoneEvent(
                contract.getId(),
                request.getMilestoneOrder(),
                request.getStatus().toUpperCase(),
                request.getRecordedBy(),
                request.getNotes()
        );

        ContractMilestoneEvent saved = milestoneEventRepository.save(event);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "MILESTONE_TRACKED");
        payload.put("contractId", contract.getId());
        payload.put("milestoneOrder", request.getMilestoneOrder());
        payload.put("status", request.getStatus().toUpperCase());
        payload.put("recordedBy", request.getRecordedBy());
        payload.put("notes", request.getNotes());

        notifyObservers("CONTRACT", payload);

        return saved;
    }

    @Cacheable(value = "contract-service::S4-F10", key = "#startDate + ':' + #endDate")
    public ContractAnalyticsDTO getContractAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59, 999000000);

        long total = analyticsRepository.countContractsInRange(start, end);
        double avgValue = analyticsRepository.avgContractValue(start, end);
        long completed = analyticsRepository.countCompletedInRange(start, end);
        double completionRate = total == 0 ? 0.0 : (double) completed / total;
        double avgDuration = analyticsRepository.avgDurationDays(start, end);
        Map<String, Long> byStatus = analyticsRepository.countByStatus(start, end);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "ANALYTICS_VIEWED");
        payload.put("startDate", startDate.toString());
        payload.put("endDate", endDate.toString());
        notifyObservers("CONTRACT", payload);

        return ContractAnalyticsDTO.builder()
                .totalContracts(total)
                .averageContractValue(avgValue)
                .completionRate(completionRate)
                .averageContractDurationDays(avgDuration)
                .contractsByStatus(byStatus)
                .build();
    }
}

