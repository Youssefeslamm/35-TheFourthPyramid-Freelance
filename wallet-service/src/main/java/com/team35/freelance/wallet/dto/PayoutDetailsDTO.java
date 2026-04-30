package com.team35.freelance.wallet.dto;

import com.team35.freelance.wallet.model.PayoutMethod;
import com.team35.freelance.wallet.model.PayoutStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PayoutDetailsDTO implements Serializable {

    private Long payoutId;
    private Long contractId;
    private Long freelancerId;
    private Double originalAmount;
    private PayoutMethod method;
    private PayoutStatus status;
    private Map<String, Object> transactionDetails;
    private List<AppliedPromoCodeDTO> appliedPromoCodes;
    private Double totalDiscount;
    private Double finalAmount;

    public PayoutDetailsDTO() {
    }

    public PayoutDetailsDTO(Long payoutId, Long contractId, Long freelancerId,
                            Double originalAmount, PayoutMethod method, PayoutStatus status,
                            Map<String, Object> transactionDetails,
                            List<AppliedPromoCodeDTO> appliedPromoCodes,
                            Double totalDiscount, Double finalAmount) {
        this.payoutId = payoutId;
        this.contractId = contractId;
        this.freelancerId = freelancerId;
        this.originalAmount = originalAmount;
        this.method = method;
        this.status = status;
        this.transactionDetails = transactionDetails;
        this.appliedPromoCodes = appliedPromoCodes;
        this.totalDiscount = totalDiscount;
        this.finalAmount = finalAmount;
    }

    public Long getPayoutId() {
        return payoutId;
    }

    public void setPayoutId(Long payoutId) {
        this.payoutId = payoutId;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Long getFreelancerId() {
        return freelancerId;
    }

    public void setFreelancerId(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    public Double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public PayoutMethod getMethod() {
        return method;
    }

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public PayoutStatus getStatus() {
        return status;
    }

    public void setStatus(PayoutStatus status) {
        this.status = status;
    }

    public Map<String, Object> getTransactionDetails() {
        return transactionDetails;
    }

    public void setTransactionDetails(Map<String, Object> transactionDetails) {
        this.transactionDetails = transactionDetails;
    }

    public List<AppliedPromoCodeDTO> getAppliedPromoCodes() {
        return appliedPromoCodes;
    }

    public void setAppliedPromoCodes(List<AppliedPromoCodeDTO> appliedPromoCodes) {
        this.appliedPromoCodes = appliedPromoCodes;
    }

    public Double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(Double totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public static class AppliedPromoCodeDTO {
        private String promoCode;
        private String discountType;
        private Double discountApplied;
        private LocalDateTime appliedAt;

        public AppliedPromoCodeDTO() {
        }

        public AppliedPromoCodeDTO(String promoCode, String discountType,
                                   Double discountApplied, LocalDateTime appliedAt) {
            this.promoCode = promoCode;
            this.discountType = discountType;
            this.discountApplied = discountApplied;
            this.appliedAt = appliedAt;
        }

        public String getPromoCode() {
            return promoCode;
        }

        public void setPromoCode(String promoCode) {
            this.promoCode = promoCode;
        }

        public String getDiscountType() {
            return discountType;
        }

        public void setDiscountType(String discountType) {
            this.discountType = discountType;
        }

        public Double getDiscountApplied() {
            return discountApplied;
        }

        public void setDiscountApplied(Double discountApplied) {
            this.discountApplied = discountApplied;
        }

        public LocalDateTime getAppliedAt() {
            return appliedAt;
        }

        public void setAppliedAt(LocalDateTime appliedAt) {
            this.appliedAt = appliedAt;
        }
    }
}
