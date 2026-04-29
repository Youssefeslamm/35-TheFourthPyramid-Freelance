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
}
