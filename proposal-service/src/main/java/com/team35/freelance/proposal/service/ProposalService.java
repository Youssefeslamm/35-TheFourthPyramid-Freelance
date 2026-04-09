package com.team35.freelance.proposal.service;

import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    public Proposal create(Proposal proposal) {
        return proposalRepository.save(proposal);
    }

    public Proposal getById(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proposal not found with id: " + id));
    }

    public List<Proposal> getAll() {
        return proposalRepository.findAll();
    }

    public Proposal update(Long id, Proposal updated) {
        Proposal existing = getById(id);
        existing.setCoverLetter(updated.getCoverLetter());
        existing.setBidAmount(updated.getBidAmount());
        existing.setEstimatedDays(updated.getEstimatedDays());
        existing.setStatus(updated.getStatus());
        existing.setMetadata(updated.getMetadata());
        existing.setAcceptedAt(updated.getAcceptedAt());
        return proposalRepository.save(existing);
    }

    public void delete(Long id) {
        getById(id);
        proposalRepository.deleteById(id);
    }
}