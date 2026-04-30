package com.team35.freelance.job.dto;

import java.io.Serializable;

public record JobProposalSummaryDTO(
        Long jobId,
        String title,
        Long totalProposals,
        Double averageBidAmount,
        Double lowestBid,
        Double highestBid
) implements Serializable {}