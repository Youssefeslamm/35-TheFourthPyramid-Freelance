package com.team35.freelance.wallet.common.refund;

public class RefundResult {

    private double amount;
    private String reasonCode;

    public RefundResult(double amount, String reasonCode) {
        this.amount = amount;
        this.reasonCode = reasonCode;
    }

    public double getAmount() {
        return amount;
    }

    public String getReasonCode() {
        return reasonCode;
    }
}