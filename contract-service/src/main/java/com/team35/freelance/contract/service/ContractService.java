package com.team35.freelance.contract.service;

import com.team35.freelance.contract.dto.ContractSummaryDTO;
import com.team35.freelance.contract.repository.ContractRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public List<ContractSummaryDTO> searchContractsByBudget(Double minAmount, Double maxAmount, String status) {
        List<Object[]> results = contractRepository.findContractsByBudgetRangeWithFreelancerInfo(minAmount, maxAmount, status);
        
        return results.stream().map(row -> new ContractSummaryDTO(
                ((Number) row[0]).longValue(),      // contractId
                (String) row[1],                    // freelancerName
                (String) row[2],                    // jobTitle
                ((Number) row[3]).doubleValue(),    // agreedAmount
                (String) row[4],                    // status
                ((Number) row[5]).intValue()        // durationDays
        )).collect(Collectors.toList());
    }
}