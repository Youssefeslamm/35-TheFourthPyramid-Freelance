package com.team35.freelance.proposal.service;
import com.team35.freelance.proposal.dto.FeeEstimateDTO;
import com.team35.freelance.proposal.dto.ProposalDetailsDTO;
import com.team35.freelance.proposal.model.MilestoneStatus;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import com.team35.freelance.proposal.repository.ProposalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.team35.freelance.proposal.model.ProposalMilestone;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.team35.freelance.proposal.dto.MilestoneDTO;

@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;

    public Proposal create(Proposal proposal) {
        return proposalRepository.save(proposal);
    }

    public Proposal getById(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));
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
    public FeeEstimateDTO estimateFee(double bidAmount, int estimatedDays) {
        if (bidAmount <= 0 || estimatedDays <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "bidAmount and estimatedDays must be positive");
        }

        double minBid = bidAmount * 0.80;
        double maxBid = bidAmount * 1.20;
        long similarCount = proposalRepository.countSimilarActiveProposals(minBid, maxBid);

        double feePercentage;
        if (similarCount <= 5) {
            feePercentage = 20.0;
        } else if (similarCount <= 15) {
            feePercentage = 15.0;
        } else {
            feePercentage = 10.0;
        }

        double platformFee = bidAmount * feePercentage / 100;
        double freelancerPayout = bidAmount - platformFee;
        double estimatedDailyRate = freelancerPayout / estimatedDays;

        return new FeeEstimateDTO(bidAmount, platformFee,
                freelancerPayout, feePercentage,
                estimatedDailyRate);
    }
    @Transactional
    public Proposal withdrawProposal(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proposal not found"));

        if (proposal.getStatus() != ProposalStatus.SUBMITTED &&
                proposal.getStatus() != ProposalStatus.SHORTLISTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only SUBMITTED or SHORTLISTED proposals can be withdrawn");
        }

        proposal.setStatus(ProposalStatus.WITHDRAWN);

        // If this was the only active proposal and job is IN_PROGRESS, revert to OPEN
        long otherActive = proposalRepository
                .countOtherActiveProposalsForJob(proposal.getJobId(), id);
        if (otherActive == 0) {
            proposalRepository.revertJobToOpen(proposal.getJobId());
        }

        return proposalRepository.save(proposal);
    }

    public ProposalDetailsDTO getProposalDetails(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proposal not found"));

        List<MilestoneDTO> milestoneDTOs = proposal.getProposalMilestones()
                .stream()
                .sorted(Comparator.comparingInt(ProposalMilestone::getMilestoneOrder))
                .map(m -> new MilestoneDTO(
                        m.getId(),
                        m.getMilestoneOrder(),
                        m.getTitle(),
                        m.getDescription(),
                        m.getAmount(),
                        m.getStatus(),
                        m.getMetadata()
                ))
                .collect(Collectors.toList());

        long completedCount = proposal.getProposalMilestones()
                .stream()
                .filter(m -> m.getStatus() == MilestoneStatus.COMPLETED
                        || m.getStatus() == MilestoneStatus.APPROVED)
                .count();

        return new ProposalDetailsDTO(
                proposal.getId(),
                proposal.getJobId(),
                proposal.getFreelancerId(),
                proposal.getStatus(),
                proposal.getBidAmount(),
                proposal.getMetadata(),
                milestoneDTOs,
                milestoneDTOs.size(),
                completedCount
        );
    }
    // S3-F2: Accept Proposal and Create Contract
    @Transactional
    public Proposal acceptProposal(Long proposalId) {
        Proposal proposal = getById(proposalId);

        if (proposal.getStatus() != ProposalStatus.SUBMITTED &&
                proposal.getStatus() != ProposalStatus.SHORTLISTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Proposal must be SUBMITTED or SHORTLISTED to be accepted");
        }

        String role = proposalRepository.findFreelancerRole(proposal.getFreelancerId());
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Freelancer not found with id: " + proposal.getFreelancerId());
        }
        if (!role.equals("FREELANCER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User is not a freelancer");
        }

        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAcceptedAt(LocalDateTime.now());

        proposalRepository.updateJobStatusToInProgress(proposal.getJobId());

        Long clientId = proposalRepository.findClientIdByJobId(proposal.getJobId());

        proposalRepository.insertContract(
                proposal.getJobId(),
                proposal.getFreelancerId(),
                clientId,
                proposal.getId(),
                proposal.getBidAmount()
        );

        return proposalRepository.save(proposal);
    }
    // S3-F4: Complete Proposal's Contract
    @Transactional
    public Proposal completeProposal(Long proposalId) {
        Proposal proposal = getById(proposalId);

        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Proposal must be ACCEPTED to be completed");
        }

        Long contractId = proposalRepository.findActiveContractIdByProposalId(proposalId);
        if (contractId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No ACTIVE contract found for this proposal");
        }

        Double agreedAmount = proposalRepository.findAgreedAmountByContractId(contractId);
        Long jobId = proposalRepository.findJobIdByContractId(contractId);
        Long freelancerId = proposalRepository.findFreelancerIdByContractId(contractId);

        proposalRepository.completeContract(contractId);
        proposalRepository.updateJobStatusToClosed(jobId);
        proposalRepository.insertPendingPayout(contractId, freelancerId, agreedAmount);

        return proposalRepository.save(proposal);
    }
}
