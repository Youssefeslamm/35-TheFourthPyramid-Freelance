package com.team35.freelance.contracts.events;

public record ProposalWithdrawnEvent(
        Long proposalId,
        Long jobId,
        Long freelancerId
) {
}
