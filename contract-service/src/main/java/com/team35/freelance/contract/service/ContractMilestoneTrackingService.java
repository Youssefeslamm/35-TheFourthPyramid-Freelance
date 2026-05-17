package com.team35.freelance.contract.service;

import com.team35.freelance.contract.cassandra.ContractMilestoneEvent;
import com.team35.freelance.contract.dto.MilestoneTrackRequestDTO;
import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.MilestoneTrackStatus;
import com.team35.freelance.contract.cassandra.ContractMilestoneEventRepository;
import com.team35.freelance.contract.repository.ContractRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
public class ContractMilestoneTrackingService {

    private final ContractRepository contractRepository;
    private final ContractMilestoneEventRepository milestoneEventRepository;
    private final ContractEventService contractEventService;

    public ContractMilestoneTrackingService(ContractRepository contractRepository,
                                            ContractMilestoneEventRepository milestoneEventRepository,
                                            ContractEventService contractEventService) {
        this.contractRepository = contractRepository;
        this.milestoneEventRepository = milestoneEventRepository;
        this.contractEventService = contractEventService;
    }

    public void trackMilestone(Long contractId, MilestoneTrackRequestDTO request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));

        MilestoneTrackStatus validatedStatus = parseStatus(request.getStatus());

        ContractMilestoneEvent event = new ContractMilestoneEvent();
        event.setContractId(contractId);
        event.setTimestamp(LocalDateTime.now());
        event.setMilestoneOrder(request.getMilestoneOrder());
        event.setStatus(validatedStatus.name());
        event.setRecordedBy(request.getRecordedBy());
        event.setNotes(request.getNotes());
        milestoneEventRepository.save(event);

        try {
            contractEventService.logMilestoneTracked(
                    contractId,
                    request.getMilestoneOrder(),
                    validatedStatus.name(),
                    request.getRecordedBy() == null ? null : request.getRecordedBy().toString(),
                    request.getNotes()
            );
        } catch (RuntimeException ignored) {
            // Mongo observability write is independent from Cassandra time-series storage.
        }
    }

    private MilestoneTrackStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        try {
            return MilestoneTrackStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid milestone status");
        }
    }
}
