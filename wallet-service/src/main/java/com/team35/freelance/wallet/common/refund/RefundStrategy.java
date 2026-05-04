package com.team35.freelance.wallet.common.refund;

import com.team35.freelance.wallet.model.Payout;

public interface RefundStrategy {

    RefundResult calculateRefund(Payout payout, RefundRequest request);
}