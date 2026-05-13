package com.team35.freelance.contracts.events;

public record JobRatedEvent(
        Long jobId,
        Long contractId,
        Double rating,
        Long ratedBy
) {
}
