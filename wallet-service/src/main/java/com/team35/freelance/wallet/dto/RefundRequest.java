package com.team35.freelance.wallet.dto;

public class RefundRequest {
    private String reason;

    public RefundRequest() {
    }

    public RefundRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}