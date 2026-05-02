package com.team35.freelance.proposal.controller;

import com.team35.freelance.proposal.dto.*;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import com.team35.freelance.proposal.service.ProposalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proposals")
public class ProposalController {

    @Autowired
    private ProposalService proposalService;

    @PostMapping
    public ResponseEntity<Proposal> create(@RequestBody Proposal proposal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.create(proposal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proposal> getById(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<Proposal>> getAll() {
        return ResponseEntity.ok(proposalService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Proposal> update(@PathVariable Long id,
                                           @RequestBody Proposal proposal) {
        return ResponseEntity.ok(proposalService.update(id, proposal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        proposalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/estimate")
    public ResponseEntity<FeeEstimateDTO> estimateFee(@RequestBody FeeEstimateRequest request) {
        FeeEstimateDTO dto = proposalService.estimateFee(
                request.getBidAmount(), request.getEstimatedDays());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}/withdraw")
    public ResponseEntity<Proposal> withdrawProposal(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.withdrawProposal(id));
    }

    @GetMapping("/{proposalId}/details")
    public ResponseEntity<ProposalDetailsDTO> getProposalDetails(@PathVariable Long proposalId) {
        return ResponseEntity.ok(proposalService.getProposalDetails(proposalId));
    }
    // S3-F2: Accept Proposal
    @PutMapping("/{proposalId}/accept")
    public ResponseEntity<Proposal> acceptProposal(@PathVariable Long proposalId) {
        return ResponseEntity.ok(proposalService.acceptProposal(proposalId));
    }
    // S3-F4: Complete Proposal's Contract
    @PutMapping("/{id}/complete")
    public ResponseEntity<Proposal> completeProposal(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.completeProposal(id));
    }
    // S3-F8: Add Milestones to Proposal
    @PostMapping("/{proposalId}/milestones")
    public ResponseEntity<Proposal> addMilestones(@PathVariable Long proposalId,
                                                  @RequestBody List<MilestoneRequest> milestones) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.addMilestones(proposalId, milestones));
    }
    // S3-F6
    @GetMapping("/analytics")
    public ResponseEntity<ProposalAnalyticsDTO> getAnalytics(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {
        return ResponseEntity.ok(proposalService.getAnalytics(startDate, endDate));
    }
    // S3-F5
    @GetMapping("/metadata/search")
    public ResponseEntity<List<Proposal>> searchByMetadata(
            @RequestParam String key,
            @RequestParam String value) {
        return ResponseEntity.ok(proposalService.filterByMetadata(key, value));
    }
    // S3-F1
    @GetMapping("/search")
    public ResponseEntity<List<Proposal>> search(
            @RequestParam(required = false) String status,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {
        return ResponseEntity.ok(
                proposalService.getProposalsByStatusAndDateRange(status, startDate, endDate));
    }
    // S3-F10
    @GetMapping("/analytics/dashboard")
    public ResponseEntity<ProposalAnalyticsDashboardDTO> getAnalyticsDashboard(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(
                proposalService.getAnalyticsDashboard(startDate, endDate));
    }
    // S3-F11: Record Freelancer-Job Interaction
    @PostMapping("/{proposalId}/record-interaction")
    public ResponseEntity<Map<String, String>> recordInteraction(
            @PathVariable Long proposalId) {
        proposalService.recordInteraction(proposalId);
        return ResponseEntity.ok(Map.of("message", "Interaction recorded successfully"));
    }

    //S3-F12
    @GetMapping("/recommendations")
    public ResponseEntity<List<JobRecommendationDTO>> getRecommendations(
            @RequestParam Long freelancerId,
            @RequestParam(defaultValue = "5") int limit) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long callerUid = (Long) auth.getCredentials();
        String callerRole = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);

        return ResponseEntity.ok(
                proposalService.getRecommendedJobs(freelancerId, limit, callerUid, callerRole));
    }
}