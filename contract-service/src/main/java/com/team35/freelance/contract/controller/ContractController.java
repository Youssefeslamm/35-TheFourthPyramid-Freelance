package com.team35.freelance.contract.controller;

import com.team35.freelance.contract.dto.BatchStatusUpdateDTO;
import com.team35.freelance.contract.dto.ContractAnalyticsDTO;
import com.team35.freelance.contract.dto.ContractMilestoneDTO;
import com.team35.freelance.contract.dto.ContractSummaryDTO;
import com.team35.freelance.contract.dto.FreelancerPerformanceDTO;
import com.team35.freelance.contract.dto.MilestoneTrackRequestDTO;
import com.team35.freelance.contract.dto.StalledContractDTO;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.ContractStatus;
import com.team35.freelance.contract.security.JwtService;
import com.team35.freelance.contract.service.ContractAnalyticsService;
import com.team35.freelance.contract.service.ContractEventService;
import com.team35.freelance.contract.service.ContractMilestoneTrackingService;
import com.team35.freelance.contract.service.ContractMilestoneTimelineService;
import com.team35.freelance.contract.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ContractAnalyticsService contractAnalyticsService;

    @Autowired
    private ContractEventService contractEventService;

    @Autowired
    private ContractMilestoneTrackingService contractMilestoneTrackingService;

    @Autowired
    private ContractMilestoneTimelineService contractMilestoneTimelineService;

    @Autowired
    private JwtService jwtService;

    @PostMapping
    public ResponseEntity<Contract> create(@RequestBody Contract contract) {
        return ResponseEntity.ok(contractService.create(contract));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ContractSummaryDTO>> searchByBudgetRange(
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(contractService.searchByBudgetRange(minAmount, maxAmount, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return contractService.findById(id)
                .map(contract -> ResponseEntity.ok(toContractMap(contract)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(contractService.findAll().stream()
                .map(this::toContractMap)
                .collect(Collectors.toList()));
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
    public ResponseEntity<Contract> updateProgress(
            @PathVariable Long contractId,
            @RequestBody Map<String, Object> updates) {
        try {
            return ResponseEntity.ok(contractService.updateProgress(contractId, updates));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getContractsInDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ContractStatus status) {
        LocalDate effectiveStart = startDate == null ? LocalDate.of(1970, 1, 1) : startDate;
        LocalDate effectiveEnd = endDate == null ? LocalDate.now() : endDate;
        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
        LocalDateTime start = effectiveStart.atStartOfDay();
        LocalDateTime end = effectiveEnd.plusDays(1).atStartOfDay();
        return ResponseEntity.ok(contractService.getContractsInDateRange(start, end, status).stream()
                .map(this::toContractMap)
                .collect(Collectors.toList()));
    }

    @PutMapping("/batch-status")
    public ResponseEntity<Map<String, Integer>> batchUpdateStatus(
            @RequestBody List<BatchStatusUpdateDTO> updates) {
        try {
            return ResponseEntity.ok(Map.of("updatedCount", contractService.batchUpdateStatus(updates)));
        } catch (ResponseStatusException e) {
            throw e;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate effectiveStart = startDate == null ? LocalDate.of(1970, 1, 1) : startDate;
        LocalDate effectiveEnd = endDate == null ? LocalDate.now().plusDays(1) : endDate;
        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
        LocalDateTime start = effectiveStart.atStartOfDay();
        LocalDateTime end = effectiveEnd.atTime(23, 59, 59);
        return ResponseEntity.ok(contractService.getFreelancerPerformance(freelancerId, start, end));
    }

    // --- S4-F9: Find Stalled Contracts ---
    @GetMapping("/stalled")
    public ResponseEntity<List<StalledContractDTO>> getStalledContracts(
            @RequestParam(required = false) Double maxProgress,
            @RequestParam(required = false) Integer stalledDays) {
        return ResponseEntity.ok(contractService.getStalledContracts(
                maxProgress == null ? 100.0 : maxProgress,
                stalledDays == null ? 0 : stalledDays));
    }

    // --- S4-F5: Metadata JSONB Filter ---
    @GetMapping("/metadata/search")
    public ResponseEntity<List<Map<String, Object>>> searchContractsByMetadata(
            @RequestParam String key,
            @RequestParam String operator,
            @RequestParam String value) throws NumberFormatException {
        try {
            return ResponseEntity.ok(contractService.searchContractsByMetadata(key, operator, value).stream()
                    .map(this::toContractMap)
                    .collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --- S4-F10: Contract Analytics ---
    @GetMapping("/analytics")
    public ResponseEntity<ContractAnalyticsDTO> getContractAnalytics(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDate effectiveStart = startDate == null ? LocalDate.of(1970, 1, 1) : startDate;
        LocalDate effectiveEnd = endDate == null ? LocalDate.now().plusDays(1) : endDate;

        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startDate must be before or equal to endDate"
            );
        }

        LocalDateTime from = effectiveStart.atStartOfDay();
        LocalDateTime to = effectiveEnd.atTime(23, 59, 59, 999_000_000);

        contractEventService.logAnalyticsViewed(from, to, extractUserId(authorizationHeader));

        return ResponseEntity.ok(contractAnalyticsService.getContractAnalytics(from, to));
    }

    // --- S4-F11: Track Contract Milestones ---
    @PostMapping("/{id}/milestones/track")
    public ResponseEntity<Void> trackMilestone(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @RequestBody MilestoneTrackRequestDTO request) {

        contractMilestoneTrackingService.trackMilestone(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // --- S4-F12: Contract Milestone Timeline ---
    @GetMapping("/{id}/milestones/timeline")
    public ResponseEntity<List<ContractMilestoneDTO>> getMilestoneTimeline(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime) {

        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startTime must be before or equal to endTime"
            );
        }

        return ResponseEntity.ok(contractMilestoneTimelineService.getTimeline(id, startTime, endTime));
    }

    private Long extractUserId(String authorizationHeader) {
        try {
            String token = authorizationHeader.substring(7).trim();
            Object userId = jwtService.extractClaims(token).get("userId");
            if (userId == null) {
                userId = jwtService.extractClaims(token).get("uid");
            }
            return userId instanceof Number ? ((Number) userId).longValue() : Long.valueOf(String.valueOf(userId));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> toContractMap(Contract contract) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", contract.getId());
        response.put("jobId", contract.getJobId());
        response.put("freelancerId", contract.getFreelancerId());
        response.put("clientId", contract.getClientId());
        response.put("proposalId", contract.getProposalId());
        response.put("agreedAmount", contract.getAgreedAmount());
        response.put("status", contract.getStatus() == null ? null : contract.getStatus().name());
        response.put("startDate", contract.getStartDate());
        response.put("endDate", contract.getEndDate());
        response.put("metadata", contract.getMetadata());
        response.put("createdAt", contract.getCreatedAt());
        return response;
    }
}
