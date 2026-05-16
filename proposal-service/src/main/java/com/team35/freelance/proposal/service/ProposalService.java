package com.team35.freelance.proposal.service;

import com.team35.freelance.contracts.feign.ContractServiceClient;
import com.team35.freelance.contracts.feign.JobServiceClient;
import com.team35.freelance.contracts.feign.UserServiceClient;
import com.team35.freelance.proposal.common.observer.EntityObserver;
import com.team35.freelance.proposal.common.observer.MongoEventLogger;
import com.team35.freelance.proposal.dto.*;
import com.team35.freelance.proposal.model.MilestoneStatus;
import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import com.team35.freelance.proposal.model.ProposalMilestone;
import com.team35.freelance.proposal.repository.*;
import com.team35.freelance.proposal.messaging.publisher.ProposalEventPublisher;
import com.team35.freelance.contracts.events.ProposalAcceptedEvent;
import com.team35.freelance.contracts.events.ProposalCompletedEvent;
import com.team35.freelance.contracts.events.ProposalWithdrawnEvent;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.team35.freelance.proposal.dto.MilestoneDTO;
import com.team35.freelance.proposal.dto.MilestoneRequest;
import com.team35.freelance.proposal.repository.ProposalMilestoneRepository;
import com.team35.freelance.proposal.dto.ProposalAnalyticsDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.team35.freelance.proposal.dto.ProposalAnalyticsDashboardDTO;
import com.team35.freelance.proposal.common.event.ProposalEvent;
import com.team35.freelance.proposal.repository.ProposalEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.neo4j.core.Neo4jClient;
import com.team35.freelance.contracts.dto.UserProfileDTO;
import com.team35.freelance.contracts.dto.JobDTO;
import com.team35.freelance.contracts.dto.ContractDTO;
import feign.FeignException;
import java.math.BigDecimal;

@Service
public class ProposalService {


    private ProposalRepository proposalRepository;
    private ProposalMilestoneRepository milestoneRepository;
    private ProposalEventRepository proposalEventRepository;
    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);


//    private final ProposalRepository proposalRepository;
//    private final ProposalMilestoneRepository milestoneRepository;
//    private final ProposalEventRepository proposalEventRepository;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final Driver neo4jDriver;
    private final ProposalEventPublisher proposalEventPublisher;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;
    private final ContractServiceClient contractServiceClient;

    public ProposalService(ProposalRepository proposalRepository,
                           ProposalMilestoneRepository milestoneRepository,
                           ProposalEventRepository proposalEventRepository,
                           MongoEventLogger mongoEventLogger,
                           Driver neo4jDriver,
                           ProposalEventPublisher proposalEventPublisher,
                           UserServiceClient userServiceClient,
                           JobServiceClient jobServiceClient,
                           ContractServiceClient contractServiceClient) {
        this.proposalRepository = proposalRepository;
        this.milestoneRepository = milestoneRepository;
        this.proposalEventRepository = proposalEventRepository;
        this.observers.add(mongoEventLogger);
        this.neo4jDriver = neo4jDriver;
        this.proposalEventPublisher = proposalEventPublisher;
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
        this.contractServiceClient = contractServiceClient;
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }
    public void registerObserver(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }

    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
    public Proposal create(Proposal proposal) {
        Proposal saved = proposalRepository.save(proposal);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "CREATED");
        payload.put("proposalId", saved.getId());

        notifyObservers("PROPOSAL", payload);

        return saved;
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
        Proposal saved = proposalRepository.save(existing);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "UPDATED");
        payload.put("proposalId", saved.getId());

        notifyObservers("PROPOSAL", payload);

        return saved;    }

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
        Proposal proposal = getById(id);

        proposalRepository.deleteById(id);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "DELETED");
        payload.put("proposalId", id);

        notifyObservers("PROPOSAL", payload);
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
            feePercentage = 0.20;
        } else if (similarCount <= 15) {
            feePercentage = 0.15;
        } else {
            feePercentage = 0.10;
        }

        double platformFee = bidAmount * feePercentage;
        double freelancerPayout = bidAmount - platformFee;
        double estimatedDailyRate = bidAmount / estimatedDays;

        return FeeEstimateDTO.builder()
                .bidAmount(bidAmount)
                .platformFee(platformFee)
                .freelancerPayout(freelancerPayout)
                .feePercentage(feePercentage)
                .estimatedDailyRate(estimatedDailyRate)
                .build();
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
    public Proposal withdrawProposal(Long id, Long callerUserId, String callerRole) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proposal not found"));
        if (!"ADMIN".equals(callerRole) &&
                (callerUserId == null || !callerUserId.equals(proposal.getFreelancerId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the proposal's freelancer or an ADMIN can withdraw this proposal");
        }
        if (proposal.getStatus() != ProposalStatus.SUBMITTED &&
                proposal.getStatus() != ProposalStatus.SHORTLISTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only SUBMITTED or SHORTLISTED proposals can be withdrawn");
        }

        proposal.setStatus(ProposalStatus.WITHDRAWN);

        Proposal saved = proposalRepository.save(proposal);

        try {
            proposalEventPublisher.publishWithdrawn(
                    new ProposalWithdrawnEvent(saved.getId(), saved.getJobId(), saved.getFreelancerId())
            );
        } catch (Exception e) {
            log.error("Failed to publish proposal.withdrawn for proposalId={}: {}", saved.getId(), e.getMessage());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "WITHDRAWN");
        payload.put("proposalId", saved.getId());

        notifyObservers("PROPOSAL", payload);

        return saved;
    }

    @Cacheable(value = "proposal-service::S3-F9", key = "#proposalId")
    public ProposalDetailsDTO getProposalDetails(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proposal not found"));

        List<MilestoneDTO> milestoneDTOs = proposal.getProposalMilestones()
                .stream()
                .sorted(Comparator.comparingInt(ProposalMilestone::getMilestoneOrder))
                .map(m -> MilestoneDTO.builder()
                        .id(m.getId())
                        .milestoneOrder(m.getMilestoneOrder())
                        .title(m.getTitle())
                        .description(m.getDescription())
                        .amount(m.getAmount())
                        .status(m.getStatus())
                        .metadata(m.getMetadata())
                        .build())
                .collect(Collectors.toList());

        long completedCount = proposal.getProposalMilestones()
                .stream()
                .filter(m -> m.getStatus() == MilestoneStatus.COMPLETED
                        || m.getStatus() == MilestoneStatus.APPROVED)
                .count();

        return ProposalDetailsDTO.builder()
                .proposalId(proposal.getId())
                .jobId(proposal.getJobId())
                .freelancerId(proposal.getFreelancerId())
                .status(proposal.getStatus())
                .bidAmount(proposal.getBidAmount())
                .metadata(proposal.getMetadata())
                .milestones(milestoneDTOs)
                .totalMilestones(milestoneDTOs.size())
                .completedMilestones(completedCount)
                .build();
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
        log.info("===== S3-F2: acceptProposal START for proposalId={}", proposalId);
        Proposal proposal = getById(proposalId);
        log.debug("Loaded proposal: id={}, status={}, freelancerId={}, jobId={}", 
                proposal.getId(), proposal.getStatus(), proposal.getFreelancerId(), proposal.getJobId());

        if (proposal.getStatus() != ProposalStatus.SUBMITTED &&
                proposal.getStatus() != ProposalStatus.SHORTLISTED) {
            log.warn("Cannot accept proposal: invalid status. proposalId={}, currentStatus={}", proposalId, proposal.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Proposal must be SUBMITTED or SHORTLISTED to be accepted");
        }

        UserProfileDTO freelancer;
        try {
            log.debug("Fetching freelancer details from user-service. freelancerId={}", proposal.getFreelancerId());
            freelancer = userServiceClient.getUserById(proposal.getFreelancerId(), null);
            log.debug("Successfully fetched freelancer: id={}, name={}, role={}, status={}",
                    freelancer.getUserId(), freelancer.getName(), freelancer.getRole(), freelancer.getStatus());
        } catch (FeignException.NotFound e) {
            log.error("Freelancer not found in user-service. freelancerId={}, error={}",
                    proposal.getFreelancerId(), e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Freelancer not found: " + proposal.getFreelancerId()
            );
        } catch (FeignException e) {
            log.error("User service call failed. freelancerId={}, error={}, status={}",
                    proposal.getFreelancerId(), e.getMessage(), e.status(), e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "User service temporarily unavailable"
            );
        }

        if (freelancer.getRole() == null ||
                !"FREELANCER".equalsIgnoreCase(freelancer.getRole())) {
            log.warn("Freelancer does not have FREELANCER role. freelancerId={}, role={}",
                    proposal.getFreelancerId(), freelancer.getRole());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Proposal freelancer must have FREELANCER role"
            );
        }

        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposal.setAcceptedAt(LocalDateTime.now());

        Proposal saved = proposalRepository.save(proposal);
        log.info("Proposal accepted and saved. proposalId={}, status={}, acceptedAt={}",
                saved.getId(), saved.getStatus(), saved.getAcceptedAt());

        try {
            log.debug("Publishing proposal.accepted event. proposalId={}, jobId={}, freelancerId={}, bidAmount={}",
                    saved.getId(), saved.getJobId(), saved.getFreelancerId(), saved.getBidAmount());
            proposalEventPublisher.publishAccepted(
                    new ProposalAcceptedEvent(
                            saved.getId(),
                            saved.getJobId(),
                            saved.getFreelancerId(),
                            BigDecimal.valueOf(saved.getBidAmount())
                    )
            );
            log.info("Successfully published proposal.accepted event. proposalId={}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to publish proposal.accepted for proposalId={}: {}", saved.getId(), e.getMessage(), e);
            // Note: We don't re-throw here because proposal is already saved. The event publishing should be retried.
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "ACCEPTED");
        payload.put("proposalId", saved.getId());

        notifyObservers("PROPOSAL", payload);
        log.info("===== S3-F2: acceptProposal END. Success for proposalId={}", proposalId);

        return saved;
    }

    // S3-F4: Complete Proposal's Contract / Saga trigger
    @Transactional
    @CacheEvict(value = {
            "proposal-service::proposal",
            "proposal-service::S3-F1",
            "proposal-service::S3-F3",
            "proposal-service::S3-F5",
            "proposal-service::S3-F6",
            "proposal-service::S3-F9"
    }, allEntries = true)
    public Proposal completeProposal(Long proposalId, Long callerUserId, String callerRole) {
        log.info("===== S3-F4: completeProposal START for proposalId={}, callerUserId={}, callerRole={}",
                proposalId, callerUserId, callerRole);
        Proposal proposal = getById(proposalId);
        log.debug("Loaded proposal: id={}, status={}, freelancerId={}, jobId={}",
                proposal.getId(), proposal.getStatus(), proposal.getFreelancerId(), proposal.getJobId());

        if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
            log.warn("Cannot complete proposal: not ACCEPTED. proposalId={}, currentStatus={}", proposalId, proposal.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Proposal must be ACCEPTED to be completed"
            );
        }

        boolean isAdmin = callerRole != null && "ADMIN".equalsIgnoreCase(callerRole);
        boolean isProposalFreelancer = callerUserId != null &&
                callerUserId.equals(proposal.getFreelancerId());
        log.debug("Authorization check: isAdmin={}, isProposalFreelancer={}", isAdmin, isProposalFreelancer);

        if (!isAdmin && !isProposalFreelancer) {
            log.warn("Unauthorized attempt to complete proposal. proposalId={}, callerUserId={}, callerRole={}",
                    proposalId, callerUserId, callerRole);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the proposal's freelancer or an ADMIN can complete this proposal"
            );
        }

        // Pre-check 1: job must exist and must not be CLOSED.
        log.debug("Pre-check 1: Validating job. jobId={}", proposal.getJobId());
        try {
            JobDTO job = jobServiceClient.getJobById(proposal.getJobId());
            log.debug("Job fetched: id={}, status={}, title={}", job.getId(), job.getStatus(), job.getTitle());

            if (job.getStatus() != null && "CLOSED".equalsIgnoreCase(job.getStatus())) {
                log.warn("Cannot complete: job is already CLOSED. jobId={}", proposal.getJobId());
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot complete proposal because job is already CLOSED"
                );
            }
        } catch (FeignException.NotFound e) {
            log.error("Job not found in job-service. jobId={}", proposal.getJobId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Job not found: " + proposal.getJobId()
            );
        } catch (FeignException e) {
            log.error("Job service call failed. jobId={}, error={}, status={}",
                    proposal.getJobId(), e.getMessage(), e.status());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Job service temporarily unavailable"
            );
        }

        // Pre-check 2: freelancer must exist and must be ACTIVE.
        log.debug("Pre-check 2: Validating freelancer. freelancerId={}", proposal.getFreelancerId());
        UserProfileDTO freelancer;
        try {
            freelancer = userServiceClient.getUserById(proposal.getFreelancerId(), null);
            log.debug("Freelancer fetched: id={}, status={}, role={}", freelancer.getUserId(), freelancer.getStatus(), freelancer.getRole());
        } catch (FeignException.NotFound e) {
            log.error("Freelancer not found in user-service. freelancerId={}", proposal.getFreelancerId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Freelancer not found: " + proposal.getFreelancerId()
            );
        } catch (FeignException e) {
            log.error("User service call failed. freelancerId={}, error={}, status={}",
                    proposal.getFreelancerId(), e.getMessage(), e.status());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "User service temporarily unavailable"
            );
        }

        if (freelancer == null ||
                freelancer.getStatus() == null ||
                !"ACTIVE".equalsIgnoreCase(freelancer.getStatus())) {
            log.warn("Freelancer is not ACTIVE. freelancerId={}, status={}",
                    proposal.getFreelancerId(), freelancer != null ? freelancer.getStatus() : "null");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot complete proposal because freelancer is not ACTIVE"
            );
        }

        // Pre-check 3: active contract must exist for this proposal.
        log.debug("Pre-check 3: Validating active contract. proposalId={}", proposalId);
        ContractDTO activeContract;
        try {
            activeContract = contractServiceClient.getActiveContractForProposal(proposalId);
            log.debug("Active contract fetched: id={}, status={}, agreedAmount={}",
                    activeContract != null ? activeContract.getId() : "null",
                    activeContract != null ? activeContract.getStatus() : "null",
                    activeContract != null ? activeContract.getAgreedAmount() : "null");
        } catch (FeignException.NotFound e) {
            log.error("No active contract found for proposal. proposalId={}", proposalId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No active contract found for this proposal"
            );
        } catch (FeignException e) {
            log.error("Contract service call failed. proposalId={}, error={}, status={}",
                    proposalId, e.getMessage(), e.status());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Contract service temporarily unavailable"
            );
        }

        if (activeContract == null || activeContract.getId() == null) {
            log.error("Active contract is null or missing id. proposalId={}", proposalId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No active contract found for this proposal"
            );
        }

        BigDecimal agreedAmount = activeContract.getAgreedAmount() != null
                ? BigDecimal.valueOf(activeContract.getAgreedAmount())
                : BigDecimal.valueOf(proposal.getBidAmount());

        proposal.setContractId(activeContract.getId());
        proposal.setStatus(ProposalStatus.COMPLETING);

        Proposal saved = proposalRepository.save(proposal);
        log.info("Proposal marked as COMPLETING. proposalId={}, contractId={}, status={}", 
                saved.getId(), saved.getContractId(), saved.getStatus());

        try {
            log.debug("Publishing proposal.completed event. proposalId={}, contractId={}, agreedAmount={}", 
                    saved.getId(), activeContract.getId(), agreedAmount);
            proposalEventPublisher.publishCompleted(
                    new ProposalCompletedEvent(
                            saved.getId(),
                            saved.getJobId(),
                            saved.getFreelancerId(),
                            activeContract.getId(),
                            agreedAmount
                    )
            );
            log.info("Successfully published proposal.completed event. proposalId={}", saved.getId());
        } catch (Exception e) {
            log.error("Failed to publish proposal.completed for proposalId={}: {}", 
                    saved.getId(), e.getMessage(), e);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "COMPLETING");
        payload.put("proposalId", saved.getId());
        payload.put("contractId", activeContract.getId());

        notifyObservers("PROPOSAL", payload);

        return saved;
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

        Proposal saved = proposalRepository.save(proposal);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "MILESTONES_ADDED");
        payload.put("proposalId", saved.getId());

        notifyObservers("PROPOSAL", payload);

        return saved;    }
    // S3-F6
    @Cacheable(value = "proposal-service::S3-F6", key = "#startDate + ':' + #endDate")
    public ProposalAnalyticsDTO getAnalytics(
            LocalDateTime startDate, LocalDateTime endDate) {
        validateDateRange(startDate, endDate);
        List<Object[]> results = proposalRepository.getAnalytics(startDate, endDate);
        Object[] row = results.get(0);

        Long total = ((Number) row[0]).longValue();
        Long accepted = ((Number) row[1]).longValue();
        Long rejected = ((Number) row[2]).longValue();
        Double totalBid = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
        Double avgBid = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
        Double rate = (total == 0) ? 0.0
                : row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;

        return ProposalAnalyticsDTO.builder()
                .totalProposals(total)
                .acceptedProposals(accepted)
                .rejectedProposals(rejected)
                .totalBidValue(totalBid)
                .averageBid(avgBid)
                .acceptanceRate(rate)
                .build();
    }

    public ProposalAnalyticsDTO getAnalytics(Map<String, String> params) {
        LocalDateTime startDate = parseStartDate(firstNonBlank(params, "startDate", "from"));
        LocalDateTime endDate = parseEndDate(firstNonBlank(params, "endDate", "to"));
        validateDateRange(startDate, endDate);

        String status = normalizeStatus(params.get("status"));
        Long freelancerId = parseLong(params.get("freelancerId"), "freelancerId");
        Long jobId = parseLong(params.get("jobId"), "jobId");

        Object[] raw = proposalRepository.getProposalAnalyticsFiltered(
                startDate, endDate, freelancerId, jobId, status);
        Object[] row = unwrapSingleRow(raw);

        long total = numberAsLong(row, 0);
        long accepted = numberAsLong(row, 3);
        long rejected = numberAsLong(row, 4);
        double averageBid = numberAsDouble(row, 1);
        double totalBidValue = total * averageBid;
        double rate = total > 0 ? (double) accepted / total : 0.0;

        return ProposalAnalyticsDTO.builder()
                .totalProposals(total)
                .acceptedProposals(accepted)
                .rejectedProposals(rejected)
                .totalBidValue(totalBidValue)
                .averageBid(averageBid)
                .acceptanceRate(rate)
                .build();
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
        validateDateRange(startDate, endDate);
        return proposalRepository.findByStatusAndDateRange(status, startDate, endDate);
    }

    public List<Proposal> searchProposals(Map<String, String> params) {
        String status = normalizeStatus(params.get("status"));
        Long freelancerId = parseLong(params.get("freelancerId"), "freelancerId");
        Long jobId = parseLong(params.get("jobId"), "jobId");
        Double minBid = parseDouble(firstNonBlank(params, "minBid", "minAmount"), "minBid");
        Double maxBid = parseDouble(firstNonBlank(params, "maxBid", "maxAmount"), "maxBid");
        LocalDateTime startDate = parseStartDate(firstNonBlank(params, "startDate", "from"));
        LocalDateTime endDate = parseEndDate(firstNonBlank(params, "endDate", "to"));

        validateDateRange(startDate, endDate);
        if (minBid != null && maxBid != null && minBid > maxBid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minBid must not be greater than maxBid");
        }

        return proposalRepository.searchProposals(
                status, freelancerId, jobId, minBid, maxBid, startDate, endDate);
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
                    Map.of(
                            "startDate", startDate.toString(),
                            "endDate", endDate.toString()
                    )
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

    public ProposalAnalyticsDashboardDTO getAnalyticsDashboard(Map<String, String> params) {
        LocalDateTime startDate = parseStartDate(firstNonBlank(params, "startDate", "from"));
        LocalDateTime endDate = parseEndDate(firstNonBlank(params, "endDate", "to"));
        validateDateRange(startDate, endDate);

        String status = normalizeStatus(params.get("status"));
        Long freelancerId = parseLong(params.get("freelancerId"), "freelancerId");
        Long jobId = parseLong(params.get("jobId"), "jobId");

        Object[] raw = proposalRepository.getProposalAnalyticsFiltered(
                startDate, endDate, freelancerId, jobId, status);
        Object[] row = unwrapSingleRow(raw);

        long totalProposals = numberAsLong(row, 0);
        double averageBidAmount = numberAsDouble(row, 1);
        double averageEstimatedDays = numberAsDouble(row, 2);
        long acceptedCount = numberAsLong(row, 3);
        long rejectedCount = numberAsLong(row, 4);
        long withdrawnCount = numberAsLong(row, 5);
        long submittedCount = numberAsLong(row, 6);
        long shortlistedCount = numberAsLong(row, 7);

        double acceptanceRate = totalProposals > 0 ? (double) acceptedCount / totalProposals : 0.0;

        Map<String, Long> proposalsByStatus = new LinkedHashMap<>();
        proposalsByStatus.put("ACCEPTED", acceptedCount);
        proposalsByStatus.put("REJECTED", rejectedCount);
        proposalsByStatus.put("WITHDRAWN", withdrawnCount);
        proposalsByStatus.put("SUBMITTED", submittedCount);
        proposalsByStatus.put("SHORTLISTED", shortlistedCount);

        return ProposalAnalyticsDashboardDTO.builder()
                .totalProposals(totalProposals)
                .acceptanceRate(acceptanceRate)
                .averageBidAmount(averageBidAmount)
                .averageEstimatedDays(averageEstimatedDays)
                .proposalsByStatus(proposalsByStatus)
                .build();
    }

    // ── S3-F11: Record Freelancer-Job Interaction ──
    public void recordInteraction(Long proposalId) {
        // b) Find proposal in PG
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proposal not found"));

        // c) Verify SUBMITTED status
        if (proposal.getStatus() != ProposalStatus.SUBMITTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Proposal is not in SUBMITTED status");
        }

        Long freelancerId = proposal.getFreelancerId();
        Long jobId = proposal.getJobId();

        String name = "Unknown";
        String title = "Unknown";
        String category = "Unknown";

        try {
            UserProfileDTO user = userServiceClient.getUserById(freelancerId, null);
            if (user != null && user.getName() != null) name = user.getName();
        } catch (FeignException e) {
            log.warn("Could not fetch user {} from user-service: {}", freelancerId, e.getMessage());
        }

        try {
            JobDTO job = jobServiceClient.getJobById(jobId);
            if (job != null) {
                if (job.getTitle() != null) title = job.getTitle();
                if (job.getCategory() != null) category = job.getCategory();
            }
        } catch (FeignException e) {
            log.warn("Could not fetch job {} from job-service: {}", jobId, e.getMessage());
        }

        // 4. Write to Neo4j using raw driver (idempotency handled by MERGE)
        try (var session = neo4jDriver.session()) {

            String cypher = "MERGE (f:Freelancer {userId: $userId}) " +
                    "ON CREATE SET f.name = $name " +
                    "MERGE (j:Job {jobId: $jobId}) " +
                    "ON CREATE SET j.title = $title, j.category = $category " +
                    "MERGE (f)-[r:PROPOSED_TO]->(j) " +
                    "ON CREATE SET r.proposalCount = 1, r.lastProposalDate = datetime(), r.recordedProposalIdsStr = $proposalId " +
                    "ON MATCH SET r.proposalCount = CASE " +
                    "  WHEN NOT (r.recordedProposalIdsStr CONTAINS $proposalId) " +
                    "  THEN r.proposalCount + 1 ELSE r.proposalCount END, " +
                    "r.lastProposalDate = CASE " +
                    "  WHEN NOT (r.recordedProposalIdsStr CONTAINS $proposalId) " +
                    "  THEN datetime() ELSE r.lastProposalDate END, " +
                    "r.recordedProposalIdsStr = CASE " +
                    "  WHEN NOT (r.recordedProposalIdsStr CONTAINS $proposalId) " +
                    "  THEN r.recordedProposalIdsStr + ',' + $proposalId " +
                    "  ELSE r.recordedProposalIdsStr END";

            session.run(cypher, org.neo4j.driver.Values.parameters(
                    "userId", freelancerId,
                    "name", name != null ? name : "Unknown",
                    "jobId", jobId,
                    "title", title,
                    "category", category,
                    "proposalId", String.valueOf(proposalId)
            ));
            log.info("Neo4j interaction recorded for freelancer {} -> job {}", freelancerId, jobId);
        } catch (Exception e) {
            log.error("Neo4j write failed: {} - {}", e.getClass().getName(), e.getMessage());
        }

        // 5. Log INTERACTION_RECORDED event via Observer
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "INTERACTION_RECORDED");
        payload.put("proposalId", proposalId);
        payload.put("freelancerId", freelancerId);
        payload.put("jobId", jobId);
        notifyObservers("PROPOSAL", payload);
    }

    // ── S3-F12: Get Recommended Jobs for Freelancer ──
    @Cacheable(value = "proposal-service::S3-F12", key = "#freelancerId + ':' + #limit")
    public List<JobRecommendationDTO> getRecommendedJobs(Long freelancerId, int limit,
                                                         Long callerUid, String callerRole) {
        if (limit < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than 0");
        }

        // 1. Ownership check
        boolean isOwner = callerUid != null && callerUid.equals(freelancerId);
        boolean isAdmin = "ADMIN".equals(callerRole);
        if (!isOwner && !isAdmin)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");

        // 2. Skip cross-service user lookup in isolated DB mode

        // 3. Read from Neo4j using raw driver
        Set<Long> myJobIds = new HashSet<>();
        List<Map<String, Object>> allInteractions = new ArrayList<>();

        try (var session = neo4jDriver.session()) {

            var result1 = session.run(
                    "MATCH (f:Freelancer {userId: $userId})-[r:PROPOSED_TO]->(j:Job) RETURN j.jobId AS jobId",
                    org.neo4j.driver.Values.parameters("userId", freelancerId));
            while (result1.hasNext()) {
                myJobIds.add(result1.next().get("jobId").asLong());
            }

            if (myJobIds.isEmpty()) return List.of();

            var result2 = session.run(
                    "MATCH (f:Freelancer)-[r:PROPOSED_TO]->(j:Job) WHERE f.userId <> $userId RETURN f.userId AS fId, j.jobId AS jobId",
                    org.neo4j.driver.Values.parameters("userId", freelancerId));
            while (result2.hasNext()) {
                var record = result2.next();
                Map<String, Object> row = new HashMap<>();
                row.put("fId", record.get("fId").asLong());
                row.put("jobId", record.get("jobId").asLong());
                allInteractions.add(row);
            }

        } catch (Exception e) {
            log.error("Neo4j read failed: {}", e.getMessage());
            return List.of();
        }

        if (myJobIds.isEmpty()) return List.of();

        // 4. Score candidate jobs
        Map<Long, Integer> scores = new HashMap<>();
        Map<Long, Set<Long>> freelancerJobs = new HashMap<>();

        for (Map<String, Object> row : allInteractions) {
            Long fId = (Long) row.get("fId");
            Long jId = (Long) row.get("jobId");
            freelancerJobs.computeIfAbsent(fId, k -> new HashSet<>()).add(jId);
        }

        for (Map.Entry<Long, Set<Long>> entry : freelancerJobs.entrySet()) {
            boolean isSimilar = entry.getValue().stream().anyMatch(myJobIds::contains);
            if (isSimilar) {
                for (Long jId : entry.getValue()) {
                    if (!myJobIds.contains(jId)) scores.merge(jId, 1, Integer::sum);
                }
            }
        }

        if (scores.isEmpty()) return List.of();

        // 5. Sort by score, apply limit
        List<Long> topJobIds = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit).map(Map.Entry::getKey).collect(Collectors.toList());

        if (topJobIds.isEmpty()) return List.of();

        return topJobIds.stream()
                .map(jid -> {
                    String jobTitle = "Unknown";
                    String jobCategory = "Unknown";
                    try {
                        JobDTO job = jobServiceClient.getJobById(jid);
                        if (job != null) {
                            if (job.getTitle() != null) jobTitle = job.getTitle();
                            if (job.getCategory() != null) jobCategory = job.getCategory();
                        }
                    } catch (FeignException e) {
                        log.warn("Could not fetch job {} from job-service: {}", jid, e.getMessage());
                    }
                    return JobRecommendationDTO.builder()
                            .jobId(jid)
                            .title(jobTitle)
                            .category(jobCategory)
                            .score(scores.get(jid))
                            .build();
                })
                .collect(Collectors.toList());
        }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must not be after endDate");
        }
    }

    private String firstNonBlank(Map<String, String> params, String... names) {
        for (String name : names) {
            String value = params.get(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        try {
            ProposalStatus.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid proposal status");
        }
    }

    private Long parseLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a number");
        }
    }

    private Double parseDouble(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a number");
        }
    }

    private LocalDateTime parseStartDate(String value) {
        return parseDateTime(value, true);
    }

    private LocalDateTime parseEndDate(String value) {
        return parseDateTime(value, false);
    }

    private LocalDateTime parseDateTime(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (Exception ignored) {
            try {
                LocalDate date = LocalDate.parse(trimmed);
                return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59, 999000000);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
            }
        }
    }

    private Object[] unwrapSingleRow(Object[] raw) {
        if (raw == null || raw.length == 0) {
            return new Object[0];
        }
        if (raw.length == 1 && raw[0] instanceof Object[] row) {
            return row;
        }
        return raw;
    }

    private long numberAsLong(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0L;
        }
        return ((Number) row[index]).longValue();
    }

    private double numberAsDouble(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0.0;
        }
        return ((Number) row[index]).doubleValue();
    }
}
