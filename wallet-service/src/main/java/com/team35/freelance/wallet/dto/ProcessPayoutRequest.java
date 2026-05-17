package com.team35.freelance.wallet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.team35.freelance.wallet.model.PayoutMethod;

public class ProcessPayoutRequest {

    private PayoutMethod method;
    private String accountLastFour;
    @JsonAlias("simulate_failure")
    private Boolean simulateFailure;

    public PayoutMethod getMethod() {
        return method;
    }

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public String getAccountLastFour() {
        return accountLastFour;
    }

    public void setAccountLastFour(String accountLastFour) {
        this.accountLastFour = accountLastFour;
    }

    public Boolean getSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(Boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
