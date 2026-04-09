package com.team35.freelance.proposal.controller;

import com.team35.freelance.proposal.model.ProposalMilestone;
import com.team35.freelance.proposal.service.ProposalMilestoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/milestones")
public class ProposalMilestoneController {

    @Autowired
    private ProposalMilestoneService milestoneService;

    @PostMapping("/proposal/{proposalId}")
    public ResponseEntity<ProposalMilestone> create(@PathVariable Long proposalId,
                                                    @RequestBody ProposalMilestone milestone) {
        return ResponseEntity.status(HttpStatus.CREATED).body(milestoneService.create(proposalId, milestone));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(milestoneService.create(proposalId, milestone));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalMilestone> getById(@PathVariable Long id) {
        return ResponseEntity.ok(milestoneService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProposalMilestone>> getAll() {
        return ResponseEntity.ok(milestoneService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProposalMilestone> update(@PathVariable Long id,
                                                    @RequestBody ProposalMilestone milestone) {
        return ResponseEntity.ok(milestoneService.update(id, milestone));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        milestoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
}
