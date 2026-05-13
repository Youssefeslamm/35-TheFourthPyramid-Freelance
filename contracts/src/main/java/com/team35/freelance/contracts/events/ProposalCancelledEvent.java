package com.team35.freelance.contracts.events;

public record ProposalCancelledEvent(
        Long proposalId,
        Long jobId,
        Long freelancerId,
        String reason
) {
}
