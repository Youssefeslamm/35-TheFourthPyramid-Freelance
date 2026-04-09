package com.team35.freelance.contract.controller;

import com.team35.freelance.contract.dto.BatchStatusUpdateDTO;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @PostMapping
    public ResponseEntity<Contract> create(@RequestBody Contract contract) {
        return ResponseEntity.ok(contractService.create(contract));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getById(@PathVariable Long id) {
        return contractService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Contract>> getAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contract> update(@PathVariable Long id, @RequestBody Contract contract) {
        try {
            return ResponseEntity.ok(contractService.update(id, contract));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<Contract> getActiveContractForUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(contractService.getActiveContractForUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{contractId}/progress")
    public ResponseEntity<Contract> updateProgress(@PathVariable Long contractId, @RequestBody Map<String, Object> updates) {
        try {
            return ResponseEntity.ok(contractService.updateProgress(contractId, updates));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
@GetMapping("/history")
public ResponseEntity<List<Contract>> getContractsInDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(required = false) String status) {
    return ResponseEntity.ok(contractService.getContractsInDateRange(startDate, endDate, status));
}

@PutMapping("/batch-status")
public ResponseEntity<Map<String, Integer>> batchUpdateStatus(@RequestBody List<BatchStatusUpdateDTO> updates) {
    try {
        int count = contractService.batchUpdateStatus(updates);
        return ResponseEntity.ok(Map.of("updatedCount", count));
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().build();
    }
}
}
