package com.team35.freelance.contract.service;

import com.team35.freelance.contract.dto.BatchStatusUpdateDTO;
import com.team35.freelance.contract.dto.ContractSummaryDTO;
import com.team35.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team35.freelance.contract.dto.StalledContractDTO;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.ContractStatus;
import com.team35.freelance.contract.repository.ContractRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public Contract create(Contract contract) { return contractRepository.save(contract); }
    public List<Contract> findAll() { return contractRepository.findAll(); }
    public Optional<Contract> findById(Long id) { return contractRepository.findById(id); }

    public Contract update(Long id, Contract updatedContract) {
        Contract existing = contractRepository.findById(id).orElseThrow(() -> new RuntimeException("Contract not found"));
        existing.setFreelancerId(updatedContract.getFreelancerId());
        existing.setClientId(updatedContract.getClientId());
        existing.setAgreedAmount(updatedContract.getAgreedAmount());
        existing.setStatus(updatedContract.getStatus());
        existing.setStartDate(updatedContract.getStartDate());
        existing.setEndDate(updatedContract.getEndDate());
        return contractRepository.save(existing);
    }

    public void delete(Long id) {
        if (!contractRepository.existsById(id)) throw new RuntimeException("Contract not found");
        contractRepository.deleteById(id);
    }

    public Contract getActiveContractForUser(Long userId) {
        if (contractRepository.checkUserExists(userId) == 0) throw new RuntimeException("User not found");
        return contractRepository.findMostRecentActiveContractByUserId(userId).orElseThrow(() -> new RuntimeException("No active contract found for this user"));
    }

    public Contract updateProgress(Long contractId, Map<String, Object> updates) {
        Contract contract = contractRepository.findById(contractId).orElseThrow(() -> new RuntimeException("Contract not found"));
        Map<String, Object> metadata = contract.getMetadata();
        if (metadata == null) metadata = new java.util.HashMap<>();
        metadata.putAll(updates);
        contract.setMetadata(metadata);
        return contractRepository.save(contract);
    }

    public List<Contract> getContractsInDateRange(LocalDateTime startDate, LocalDateTime endDate, ContractStatus status) {
        return contractRepository.findContractsInDateRange(startDate, endDate, status);
    }

    public List<ContractSummaryDTO> searchByBudgetRange(Double minAmount, Double maxAmount, String status) {
        if (minAmount == null || maxAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minAmount and maxAmount are required");
        }
        if (minAmount > maxAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minAmount must be less than or equal to maxAmount");
        }
        String statusFilter = (status == null || status.isBlank())
                ? null
                : status.trim().toUpperCase();
        List<Object[]> rows = contractRepository.searchContractsByBudgetRange(minAmount, maxAmount, statusFilter);
        return rows.stream().map(this::toContractSummaryDTO).collect(Collectors.toList());
    }

    private ContractSummaryDTO toContractSummaryDTO(Object[] row) {
        ContractSummaryDTO dto = new ContractSummaryDTO();
        dto.setContractId(((Number) row[0]).longValue());
        dto.setFreelancerName((String) row[1]);
        dto.setJobTitle((String) row[2]);
        dto.setAgreedAmount(((Number) row[3]).doubleValue());
        dto.setStatus((String) row[4]);
        dto.setDurationDays(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0);
        return dto;
    }

    @Transactional
    public int batchUpdateStatus(List<BatchStatusUpdateDTO> updates) {
        List<Long> ids = updates.stream().map(BatchStatusUpdateDTO::getContractId).collect(Collectors.toList());
        List<Contract> contracts = contractRepository.findAllById(ids);
        if (contracts.size() != ids.size()) throw new RuntimeException("One or more contracts not found");

        for (BatchStatusUpdateDTO update : updates) {
            Contract contract = contracts.stream().filter(c -> c.getId().equals(update.getContractId())).findFirst().orElseThrow(() -> new RuntimeException("Contract not found"));
            ContractStatus newStatus;
            try {
                newStatus = ContractStatus.valueOf(update.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + update.getStatus());
            }            if (contract.getStatus() != ContractStatus.ACTIVE) throw new RuntimeException("Contract is not ACTIVE");
            if (newStatus == ContractStatus.COMPLETED && contract.getEndDate() == null) contract.setEndDate(LocalDateTime.now());
            contract.setStatus(newStatus);
        }
        contractRepository.saveAll(contracts);
        return contracts.size();
    }

    @Transactional
    public int purgeOldContracts(int olderThanDays) {
        return contractRepository.purgeOldContracts(LocalDateTime.now().minusDays(olderThanDays));
    }

    // --- S4-F8: Freelancer Performance ---
    public FreelancerPerformanceDTO getFreelancerPerformance(Long freelancerId, LocalDateTime startDate, LocalDateTime endDate) {
        if (contractRepository.checkUserExists(freelancerId) == 0) throw new RuntimeException("Freelancer not found");
        List<Object[]> results = contractRepository.getFreelancerPerformanceAggregates(freelancerId, startDate, endDate);
        Object[] row = results.get(0);

        Integer totalContracts = ((Number) row[0]).intValue();
        Integer completedContracts = ((Number) row[1]).intValue();
        Double totalAmount = ((Number) row[2]).doubleValue();
        Double totalEarnings = ((Number) row[3]).doubleValue();
        Double avgDuration = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

        Double averageContractValue = totalContracts > 0 ? totalAmount / totalContracts : 0.0;
        Double completionRate = totalContracts > 0 ? ((double) completedContracts / totalContracts) * 100 : 0.0;

        return new FreelancerPerformanceDTO(freelancerId, totalContracts, averageContractValue, completionRate, avgDuration, totalEarnings);
    }

    // --- S4-F9: Find Stalled Contracts ---
    public List<StalledContractDTO> getStalledContracts(Double maxProgress, Integer stalledDays) {
        List<Object[]> results = contractRepository.findStalledContracts(maxProgress, stalledDays);
        return results.stream().map(row -> new StalledContractDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).doubleValue(),
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0,
                ((Number) row[5]).intValue()
        )).collect(Collectors.toList());
    }
    // --- S4-F5: Metadata JSONB Filter ---
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
}