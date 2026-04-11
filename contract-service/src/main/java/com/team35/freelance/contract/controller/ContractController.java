package com.team35.freelance.contract.controller;

import com.team35.freelance.contract.dto.BatchStatusUpdateDTO;
import com.team35.freelance.contract.dto.ContractSummaryDTO;
import com.team35.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team35.freelance.contract.dto.StalledContractDTO;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.ContractStatus;
import com.team35.freelance.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/search")
    public ResponseEntity<List<ContractSummaryDTO>> searchByBudgetRange(
            @RequestParam Double minAmount,
            @RequestParam Double maxAmount,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(contractService.searchByBudgetRange(minAmount, maxAmount, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getById(@PathVariable Long id) {
        return contractService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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
            @RequestParam(required = false) ContractStatus status) {
        return ResponseEntity.ok(contractService.getContractsInDateRange(startDate, endDate, status));
    }

    @PutMapping("/batch-status")
    public ResponseEntity<Map<String, Integer>> batchUpdateStatus(@RequestBody List<BatchStatusUpdateDTO> updates) {
        try {
            return ResponseEntity.ok(Map.of("updatedCount", contractService.batchUpdateStatus(updates)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purgeOldContracts(@RequestParam int olderThanDays) {
        return ResponseEntity.ok(Map.of("deletedCount", contractService.purgeOldContracts(olderThanDays)));
    }

    // --- S4-F8: Freelancer Performance ---
    @GetMapping("/freelancer/{freelancerId}/summary")
    public ResponseEntity<FreelancerPerformanceDTO> getFreelancerSummary(
            @PathVariable Long freelancerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(23, 59, 59);
            return ResponseEntity.ok(contractService.getFreelancerPerformance(freelancerId, start, end));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // --- S4-F9: Find Stalled Contracts ---
    @GetMapping("/stalled")
    public ResponseEntity<List<StalledContractDTO>> getStalledContracts(
            @RequestParam Double maxProgress,
            @RequestParam Integer stalledDays) {
        return ResponseEntity.ok(contractService.getStalledContracts(maxProgress, stalledDays));
    }
    // --- S4-F5: Metadata JSONB Filter ---
    @GetMapping("/metadata/search")
    public ResponseEntity<List<Contract>> searchContractsByMetadata(
            @RequestParam String key,
            @RequestParam String operator,
            @RequestParam String value) throws NumberFormatException {
        try {
            return ResponseEntity.ok(contractService.searchContractsByMetadata(key, operator, value));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // Throws 400 if operator or number cast is invalid
        }
    }

}
