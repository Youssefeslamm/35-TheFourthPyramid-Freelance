package com.team35.freelance.job.dto;

public record JobProposalSummaryDTO(
        Long jobId,
        String title,
        Long totalProposals,
        Double averageBidAmount,
        Double lowestBid,
        Double highestBid
) {}