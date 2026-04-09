package com.team35.freelance.contract.service;

import com.team35.freelance.contract.dto.BatchStatusUpdateDTO;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.ContractStatus;
import com.team35.freelance.contract.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Transactional
    public int batchStatusUpdate(List<BatchStatusUpdateDTO> updates) {
        for (BatchStatusUpdateDTO dto : updates) {
            Contract contract = contractRepository.findById(dto.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contract not found: " + dto.getContractId()));
            ContractStatus newStatus = ContractStatus.valueOf(dto.getStatus());
            if (contract.getStatus() == ContractStatus.COMPLETED) {
                throw new RuntimeException("Cannot update a COMPLETED contract");
            }
            contract.setStatus(newStatus);
            if (newStatus == ContractStatus.COMPLETED) {
                contract.setEndDate(LocalDateTime.now());
            }
            contractRepository.save(contract);
        }
        return updates.size();
    }
}
