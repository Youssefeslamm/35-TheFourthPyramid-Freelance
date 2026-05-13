package com.team35.freelance.contracts.events;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String role
) {
}
