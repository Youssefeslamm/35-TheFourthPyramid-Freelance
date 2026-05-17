package com.team35.freelance.contracts.events;

import java.math.BigDecimal;

public record ContractCreatedEvent(
        Long contractId,
        Long proposalId,
        Long jobId,
        Long freelancerId,
        BigDecimal agreedAmount
) {
}
