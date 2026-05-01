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
import com.team35.freelance.proposal.dto.MilestoneRequest;
import com.team35.freelance.proposal.repository.ProposalMilestoneRepository;
import java.util.HashMap;
import com.team35.freelance.proposal.dto.ProposalAnalyticsDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.team35.freelance.proposal.dto.ProposalAnalyticsDashboardDTO;
import com.team35.freelance.proposal.model.ProposalEvent;
import com.team35.freelance.proposal.repository.ProposalEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProposalService {

    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private ProposalMilestoneRepository milestoneRepository;
    @Autowired
    private ProposalEventRepository proposalEventRepository;
    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);
    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
    public Proposal create(Proposal proposal) {
        return proposalRepository.save(proposal);
    }


    @Cacheable(value = "proposal-service::proposal", key = "#id")
    public Proposal getById(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));
                  }

    public List<Proposal> getAll() {
        return proposalRepository.findAll();
    }


    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
    public Proposal update(Long id, Proposal updated) {
        Proposal existing = getProposalEntity(id);
        existing.setCoverLetter(updated.getCoverLetter());
        existing.setBidAmount(updated.getBidAmount());
        existing.setEstimatedDays(updated.getEstimatedDays());
        existing.setStatus(updated.getStatus());
        existing.setMetadata(updated.getMetadata());
        existing.setAcceptedAt(updated.getAcceptedAt());
        return proposalRepository.save(existing);
    }

    private Proposal getProposalEntity(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposal not found"));
    }


    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
    public void delete(Long id) {
        getById(id);
        proposalRepository.deleteById(id);
    }

    @Cacheable(value = "proposal-service::S3-F3", key = "#bidAmount + ':' + #estimatedDays")
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
    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
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

    @Cacheable(value = "proposal-service::S3-F9", key = "#proposalId")
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
    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
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
    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
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
    // S3-F8: Add Milestones to Proposal
    @Transactional
    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
    public Proposal addMilestones(Long proposalId, List<MilestoneRequest> milestoneRequests) {
        Proposal proposal = getById(proposalId);

        if (proposal.getStatus() != ProposalStatus.SUBMITTED &&
                proposal.getStatus() != ProposalStatus.SHORTLISTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot add milestones to a proposal that is not SUBMITTED or SHORTLISTED");
        }

        for (MilestoneRequest req : milestoneRequests) {
            if (req.getTitle() == null || req.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each milestone must have a title");
            }
            if (req.getDescription() == null || req.getDescription().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each milestone must have a description");
            }
            if (req.getAmount() == null || req.getAmount() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each milestone must have a positive amount");
            }
        }

        Integer currentMax = milestoneRepository.findMaxMilestoneOrderByProposalId(proposalId);
        Double existingTotal = milestoneRepository.findTotalMilestoneAmountByProposalId(proposalId);

        double newTotal = milestoneRequests.stream()
                .mapToDouble(MilestoneRequest::getAmount)
                .sum();

        if (existingTotal + newTotal > proposal.getBidAmount()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Total milestone amounts exceed the proposal bid amount of " + proposal.getBidAmount());
        }

        int order = currentMax + 1;
        for (MilestoneRequest req : milestoneRequests) {
            ProposalMilestone milestone = new ProposalMilestone();
            milestone.setTitle(req.getTitle());
            milestone.setDescription(req.getDescription());
            milestone.setAmount(req.getAmount());
            milestone.setStatus(MilestoneStatus.PENDING);
            milestone.setMilestoneOrder(order++);
            milestone.setMetadata(req.getMetadata() != null ? req.getMetadata() : new HashMap<>());
            milestone.setProposal(proposal);
            proposal.getProposalMilestones().add(milestone);
        }

        return proposalRepository.save(proposal);
    }
    // S3-F6
    @Cacheable(value = "proposal-service::S3-F6", key = "#startDate + ':' + #endDate")
    public ProposalAnalyticsDTO getAnalytics(
            LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = proposalRepository.getAnalytics(startDate, endDate);
        Object[] row = results.get(0);

        Long total = ((Number) row[0]).longValue();
        Long accepted = ((Number) row[1]).longValue();
        Long rejected = ((Number) row[2]).longValue();
        Double totalBid = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        Double avgBid = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
        Double rate = (total == 0) ? 0.0
                : row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;

        return new ProposalAnalyticsDTO(
                total, accepted, rejected, totalBid, avgBid, rate);
    }
    // S3-F5

    @Cacheable(value = "proposal-service::S3-F5", key = "#key + ':' + #value")
    public List<Proposal> filterByMetadata(String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Key and value must not be blank");
        }
        return proposalRepository.findByMetadataField(key, value);
    }
    // S3-F1

    @Cacheable(value = "proposal-service::S3-F1", key = "#status + ':' + #startDate + ':' + #endDate")
    public List<Proposal> getProposalsByStatusAndDateRange(
            String status, LocalDateTime startDate, LocalDateTime endDate) {
        if (status != null && status.isBlank()) {
            status = null;
        }
        return proposalRepository.findByStatusAndDateRange(status, startDate, endDate);
    }
    public ProposalAnalyticsDashboardDTO getAnalyticsDashboard(
            LocalDate startDate, LocalDate endDate) {

        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startDate must not be after endDate");
        }

        // Expand to full day range
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59, 999000000);

        // Query PostgreSQL
        Object[] result = proposalRepository.getProposalAnalytics(start, end);
        Object[] row = (Object[]) result[0];

        long totalProposals = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        double averageBidAmount = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double averageEstimatedDays = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        long acceptedCount = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        long rejectedCount = row[4] != null ? ((Number) row[4]).longValue() : 0L;
        long withdrawnCount = row[5] != null ? ((Number) row[5]).longValue() : 0L;
        long submittedCount = row[6] != null ? ((Number) row[6]).longValue() : 0L;
        long shortlistedCount = row[7] != null ? ((Number) row[7]).longValue() : 0L;

        double acceptanceRate = totalProposals > 0
                ? (double) acceptedCount / totalProposals : 0.0;

        Map<String, Long> proposalsByStatus = new LinkedHashMap<>();
        if (acceptedCount > 0) proposalsByStatus.put("ACCEPTED", acceptedCount);
        if (rejectedCount > 0) proposalsByStatus.put("REJECTED", rejectedCount);
        if (withdrawnCount > 0) proposalsByStatus.put("WITHDRAWN", withdrawnCount);
        if (submittedCount > 0) proposalsByStatus.put("SUBMITTED", submittedCount);
        if (shortlistedCount > 0) proposalsByStatus.put("SHORTLISTED", shortlistedCount);

        // Log ANALYTICS_VIEWED to MongoDB
        try {
            ProposalEvent event = new ProposalEvent(
                    null,
                    "ANALYTICS_VIEWED",
                    LocalDateTime.now(),
                    Map.of("startDate", startDate.toString(),
                            "endDate", endDate.toString())
            );
            proposalEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to log analytics event to MongoDB: {}", e.getMessage());
        }

        // Build and return DTO
        return ProposalAnalyticsDashboardDTO.builder()
                .totalProposals(totalProposals)
                .acceptanceRate(acceptanceRate)
                .averageBidAmount(averageBidAmount)
                .averageEstimatedDays(averageEstimatedDays)
                .proposalsByStatus(proposalsByStatus)
                .build();
    }
}

