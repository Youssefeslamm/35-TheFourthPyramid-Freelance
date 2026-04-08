package com.team35.freelance.contract.controller;

import com.team35.freelance.contract.dto.ContractSummaryDTO;
import com.team35.freelance.contract.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ContractSummaryDTO>> searchContracts(
            @RequestParam Double minAmount,
            @RequestParam Double maxAmount,
            @RequestParam(required = false) String status) {
        
        List<ContractSummaryDTO> contracts = contractService.searchContractsByBudget(minAmount, maxAmount, status);
        return ResponseEntity.ok(contracts);
    }
}