package com.team35.freelance.wallet.common.refund;

public class RefundRequest {

    private String reversalScope;

    public RefundRequest(String reversalScope) {
        this.reversalScope = reversalScope;
    }

    public String getReversalScope() {
        return reversalScope;
    }
}