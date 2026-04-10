package com.team35.freelance.wallet.dto;

import java.util.Map;

public class FreelancerPayoutSummaryDTO {

    private Long freelancerId;
    private Long totalPayouts;
    private Double totalAmount;
    private Map<String, Double> methodBreakdown;

    // Constructor
    public FreelancerPayoutSummaryDTO(Long freelancerId,
                                      Long totalPayouts,
                                      Double totalAmount,
                                      Map<String, Double> methodBreakdown) {
        this.freelancerId = freelancerId;
        this.totalPayouts = totalPayouts;
        this.totalAmount = totalAmount;
        this.methodBreakdown = methodBreakdown;
    }

    // Getters
    public Long getFreelancerId() {
        return freelancerId;
    }

    public Long getTotalPayouts() {
        return totalPayouts;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public Map<String, Double> getMethodBreakdown() {
        return methodBreakdown;
    }

    // Setters
    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public void setTotalPayouts(Long totalPayouts) {
        this.totalPayouts = totalPayouts;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setMethodBreakdown(Map<String, Double> methodBreakdown) {
        this.methodBreakdown = methodBreakdown;
    }
}