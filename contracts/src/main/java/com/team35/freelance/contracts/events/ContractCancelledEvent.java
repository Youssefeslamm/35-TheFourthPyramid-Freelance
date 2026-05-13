package com.team35.freelance.contracts.events;

public record ContractCancelledEvent(
        Long contractId,
        Long proposalId
) {
}
