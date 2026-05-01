package com.team35.freelance.wallet.common.refund;

import com.team35.freelance.wallet.model.Payout;
import org.springframework.stereotype.Component;

@Component
public class NoReversalStrategy implements RefundStrategy {

    @Override
    public RefundResult calculateRefund(Payout payout, RefundRequest request) {
        return new RefundResult(
                0.0,
                "REVERSAL_WINDOW_EXPIRED"
        );
    }
}