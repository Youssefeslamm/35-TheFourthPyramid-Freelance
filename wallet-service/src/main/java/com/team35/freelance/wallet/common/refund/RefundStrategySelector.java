package com.team35.freelance.wallet.common.refund;

import com.team35.freelance.wallet.model.Payout;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RefundStrategySelector {

    private final FullPayoutReversalStrategy fullStrategy;
    private final MilestoneReversalStrategy milestoneStrategy;
    private final NoReversalStrategy noReversalStrategy;

    public RefundStrategySelector(
            FullPayoutReversalStrategy fullStrategy,
            MilestoneReversalStrategy milestoneStrategy,
            NoReversalStrategy noReversalStrategy
    ) {
        this.fullStrategy = fullStrategy;
        this.milestoneStrategy = milestoneStrategy;
        this.noReversalStrategy = noReversalStrategy;
    }

    public RefundStrategy select(Payout payout, RefundRequest request) {

        boolean expired = payout.getCreatedAt()
                .isBefore(LocalDateTime.now().minusDays(30));

        if (expired) {
            return noReversalStrategy;
        }

        if ("FULL".equalsIgnoreCase(request.getReversalScope())) {
            return fullStrategy;
        }

        if ("MILESTONE_ONLY".equalsIgnoreCase(request.getReversalScope())) {
            return milestoneStrategy;
        }

        throw new RuntimeException("Invalid reversal scope");
    }
}