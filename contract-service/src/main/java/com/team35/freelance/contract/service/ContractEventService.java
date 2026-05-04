package com.team35.freelance.contract.service;

import com.team35.freelance.contract.document.ContractEvent;
import com.team35.freelance.contract.repository.ContractEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ContractEventService {

    private final ContractEventRepository contractEventRepository;

    public ContractEventService(ContractEventRepository contractEventRepository) {
        this.contractEventRepository = contractEventRepository;
    }

    public void logAnalyticsViewed(LocalDateTime startDate, LocalDateTime endDate) {
        ContractEvent event = new ContractEvent();
        event.setEventType("ANALYTICS_VIEWED");
        event.setOccurredAt(LocalDateTime.now());
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        contractEventRepository.save(event);
    }

    public void logMilestoneTracked(Long contractId,
                                    Integer milestoneOrder,
                                    String status,
                                    String recordedBy,
                                    String notes) {
        ContractEvent event = new ContractEvent();
        event.setEventType("MILESTONE_TRACKED");
        event.setOccurredAt(LocalDateTime.now());
        event.setContractId(contractId);
        event.setMilestoneOrder(milestoneOrder);
        event.setStatus(status);
        event.setDetails("recordedBy=" + recordedBy + ", notes=" + notes);
        contractEventRepository.save(event);
    }
}
