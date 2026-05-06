package com.team35.freelance.wallet.common.refund;

import com.team35.freelance.wallet.model.Payout;
import org.springframework.stereotype.Component;

@Component
public class FullPayoutReversalStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(Payout payout, RefundRequest request) {
        return new RefundResult(
                payout.getAmount() == null ? 0.0 : payout.getAmount(),
                "FULL_REVERSAL"
        );
    }
}
