package com.team35.freelance.contracts.events;

public record JobClosedEvent(
        Long jobId,
        Long clientId
) {
}
