package com.team35.freelance.proposal.controller;

import com.team35.freelance.proposal.dto.FeeEstimateDTO;
import com.team35.freelance.proposal.dto.FeeEstimateRequest;
import com.team35.freelance.proposal.dto.ProposalDetailsDTO;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.service.ProposalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}