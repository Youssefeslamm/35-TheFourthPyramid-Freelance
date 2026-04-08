package com.team35.freelance.proposal.service;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalMilestone;
import com.team35.freelance.proposal.repository.ProposalMilestoneRepository;
import com.team35.freelance.proposal.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProposalMilestoneService {
    @Autowired
    private ProposalMilestoneRepository milestoneRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    public ProposalMilestone create(Long proposalId, ProposalMilestone milestone) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));
        milestone.setProposal(proposal);
        return milestoneRepository.save(milestone);
    }

    public ProposalMilestone getById(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
    }

    public List<ProposalMilestone> getAll() {
        return milestoneRepository.findAll();
    }

    public ProposalMilestone update(Long id, ProposalMilestone updated) {
        ProposalMilestone existing = getById(id);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setAmount(updated.getAmount());
        existing.setStatus(updated.getStatus());
        existing.setMilestoneOrder(updated.getMilestoneOrder());
        existing.setMetadata(updated.getMetadata());
        return milestoneRepository.save(existing);
    }

    public void delete(Long id) {
        getById(id);
        milestoneRepository.deleteById(id);
    }
}
