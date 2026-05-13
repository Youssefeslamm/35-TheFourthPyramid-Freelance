package com.team35.freelance.contracts.events;

public record ContractStatusChangedEvent(
        Long contractId,
        String oldStatus,
        String newStatus
) {
}
