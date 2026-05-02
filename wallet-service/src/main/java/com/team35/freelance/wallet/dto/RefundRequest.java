package com.team35.freelance.wallet.dto;

public class RefundRequest {

    private String reason;
    private String reversalScope;

    public RefundRequest() {}

    public String getReason() {
        return reason;
    }

    public String getReversalScope() {
        return reversalScope;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setReversalScope(String reversalScope) {
        this.reversalScope = reversalScope;
    }
}