package com.team35.freelance.wallet.dto;

public class FreelancerPayoutSummaryDTO {

    private Long freelancerId;
    private Long totalPayouts;
    private Long completedPayouts;
    private Long failedPayouts;
    private Long refundedPayouts;
    private Double totalEarnings;
    private Double averagePayout;

    public FreelancerPayoutSummaryDTO(Long freelancerId,
                                      Long totalPayouts,
                                      Long completedPayouts,
                                      Long failedPayouts,
                                      Long refundedPayouts,
                                      Double totalEarnings,
                                      Double averagePayout) {
        this.freelancerId = freelancerId;
        this.totalPayouts = totalPayouts;
        this.completedPayouts = completedPayouts;
        this.failedPayouts = failedPayouts;
        this.refundedPayouts = refundedPayouts;
        this.totalEarnings = totalEarnings;
        this.averagePayout = averagePayout;
    }

    public Long getFreelancerId() { return freelancerId; }
    public Long getTotalPayouts() { return totalPayouts; }
    public Long getCompletedPayouts() { return completedPayouts; }
    public Long getFailedPayouts() { return failedPayouts; }
    public Long getRefundedPayouts() { return refundedPayouts; }
    public Double getTotalEarnings() { return totalEarnings; }
    public Double getAveragePayout() { return averagePayout; }
}