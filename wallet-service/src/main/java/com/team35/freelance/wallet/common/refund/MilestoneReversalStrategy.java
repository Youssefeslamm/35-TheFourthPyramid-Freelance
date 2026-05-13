package com.team35.freelance.wallet.common.refund;

import com.team35.freelance.wallet.model.Payout;
import org.springframework.stereotype.Component;

@Component
public class MilestoneReversalStrategy implements RefundStrategy {

    public MilestoneReversalStrategy() {
    }

    @Override
    public RefundResult calculateRefund(Payout payout, RefundRequest request) {

        Long contractId = payout.getContractId();

        if (contractId == null) {
            throw new RuntimeException("Payout not linked to contract");
        }

        double refundAmount = 0.0;
        return new RefundResult(refundAmount, "MILESTONE_ONLY");
    }
}