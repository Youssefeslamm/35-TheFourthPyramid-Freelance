package com.team35.freelance.contract.service;

import com.team35.freelance.contract.dto.ContractSummaryDTO;
import com.team35.freelance.contract.model.Contract;
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

    public Contract create(Contract contract) {
        return contractRepository.save(contract);
    }

    public List<Contract> findAll() {
        return contractRepository.findAll();
    }

    public Optional<Contract> findById(Long id) {
        return contractRepository.findById(id);
    }

    public Contract update(Long id, Contract updatedContract) {
        Contract existing = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        existing.setFreelancerId(updatedContract.getFreelancerId());
        existing.setClientId(updatedContract.getClientId());
        existing.setAgreedAmount(updatedContract.getAgreedAmount());
        existing.setStatus(updatedContract.getStatus());
        existing.setStartDate(updatedContract.getStartDate());
        existing.setEndDate(updatedContract.getEndDate());
        return contractRepository.save(existing);
    }

    public void delete(Long id) {
        if (!contractRepository.existsById(id)) {
            throw new RuntimeException("Contract not found");
        }
        contractRepository.deleteById(id);
    }

    public Contract getActiveContractForUser(Long userId) {
        int exists = contractRepository.checkUserExists(userId);
        if (exists == 0) {
            throw new RuntimeException("User not found");
        }
        return contractRepository.findMostRecentActiveContractByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No active contract found for this user"));
    }

    public Contract updateProgress(Long contractId, Map<String, Object> updates) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        Map<String, Object> metadata = contract.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.putAll(updates);
        contract.setMetadata(metadata);
        return contractRepository.save(contract);
    }

    public List<Contract> getContractsInDateRange(LocalDateTime startDate, LocalDateTime endDate, String status) {
        return contractRepository.findContractsInDateRange(startDate, endDate, status);
    }

    public List<ContractSummaryDTO> searchByBudgetRange(Double minAmount, Double maxAmount, String status) {
        if (minAmount == null || maxAmount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minAmount and maxAmount are required");
        }
        if (minAmount > maxAmount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minAmount must be less than or equal to maxAmount");
        }
        String statusFilter = (status == null || status.isBlank()) ? null : status.trim();
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
    public int purgeOldContracts(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        return contractRepository.purgeOldContracts(cutoffDate);
    }
}
