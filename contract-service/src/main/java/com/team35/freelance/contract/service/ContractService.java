package com.team35.freelance.contract.service;

import com.team35.freelance.contract.dto.BatchStatusUpdateDTO;
import com.team35.freelance.contract.dto.StalledContractDTO;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.ContractStatus;
import com.team35.freelance.contract.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public int batchUpdateStatus(List<BatchStatusUpdateDTO> updates) {
        List<Long> ids = updates.stream().map(BatchStatusUpdateDTO::getContractId).collect(Collectors.toList());
        List<Contract> contracts = contractRepository.findAllById(ids);
        
        if (contracts.size() != ids.size()) {
            throw new RuntimeException("One or more contracts not found");
        }
        
        for (BatchStatusUpdateDTO update : updates) {
            Contract contract = contracts.stream()
                    .filter(c -> c.getId().equals(update.getContractId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Contract not found: " + update.getContractId()));
            
            ContractStatus newStatus = ContractStatus.valueOf(update.getStatus());
            ContractStatus currentStatus = contract.getStatus();
            
            if (currentStatus != ContractStatus.ACTIVE) {
                throw new RuntimeException("Contract " + contract.getId() + " is not ACTIVE, cannot change status");
            }
            if (newStatus == ContractStatus.COMPLETED && contract.getEndDate() == null) {
                contract.setEndDate(LocalDateTime.now());
            }
            
            contract.setStatus(newStatus);
        }
        
        contractRepository.saveAll(contracts);
        return contracts.size();
    }

    @Transactional
    public int purgeOldContracts(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        return contractRepository.purgeOldContracts(cutoffDate);
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
}
