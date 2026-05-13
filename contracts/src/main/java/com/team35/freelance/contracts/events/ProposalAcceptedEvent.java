package com.team35.freelance.contracts.events;

import java.math.BigDecimal;

public record ProposalAcceptedEvent(
        Long proposalId,
        Long jobId,
        Long freelancerId,
        BigDecimal bidAmount
) {
}
