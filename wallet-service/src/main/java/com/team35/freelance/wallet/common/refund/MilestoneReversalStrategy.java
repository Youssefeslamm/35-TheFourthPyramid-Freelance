package com.team35.freelance.wallet.common.refund;

import com.team35.freelance.wallet.model.Payout;
import org.springframework.stereotype.Component;

@Component
public class MilestoneReversalStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(Payout payout, RefundRequest request) {

        double partialAmount = payout.getAmount() * 0.5;

        return new RefundResult(
                partialAmount,
                "MILESTONE_REVERSAL"
        );
    }
}