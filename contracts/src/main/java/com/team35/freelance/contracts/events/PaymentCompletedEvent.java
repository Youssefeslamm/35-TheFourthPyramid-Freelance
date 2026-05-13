package com.team35.freelance.contracts.events;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        Long payoutId,
        Long proposalId,
        Long contractId,
        BigDecimal amount
) {
}
