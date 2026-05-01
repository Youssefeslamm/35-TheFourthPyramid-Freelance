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

    private PayoutDetailsDTO(Builder builder) {
        this.payoutId = builder.payoutId;
        this.contractId = builder.contractId;
        this.freelancerId = builder.freelancerId;
        this.originalAmount = builder.originalAmount;
        this.method = builder.method;
        this.status = builder.status;
        this.transactionDetails = builder.transactionDetails;
        this.appliedPromoCodes = builder.appliedPromoCodes;
        this.totalDiscount = builder.totalDiscount;
        this.finalAmount = builder.finalAmount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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

        public Builder payoutId(Long val) { this.payoutId = val; return this; }
        public Builder contractId(Long val) { this.contractId = val; return this; }
        public Builder freelancerId(Long val) { this.freelancerId = val; return this; }
        public Builder originalAmount(Double val) { this.originalAmount = val; return this; }
        public Builder method(PayoutMethod val) { this.method = val; return this; }
        public Builder status(PayoutStatus val) { this.status = val; return this; }
        public Builder transactionDetails(Map<String, Object> val) { this.transactionDetails = val; return this; }
        public Builder appliedPromoCodes(List<AppliedPromoCodeDTO> val) { this.appliedPromoCodes = val; return this; }
        public Builder totalDiscount(Double val) { this.totalDiscount = val; return this; }
        public Builder finalAmount(Double val) { this.finalAmount = val; return this; }

        public PayoutDetailsDTO build() {
            return new PayoutDetailsDTO(this);
        }
    }

    public Long getPayoutId() { return payoutId; }
    public Long getContractId() { return contractId; }

    public Long getFreelancerId() { return freelancerId; }

    public Double getOriginalAmount() { return originalAmount; }

    public PayoutMethod getMethod() { return method; }

    public PayoutStatus getStatus() { return status; }

    public Map<String, Object> getTransactionDetails() { return transactionDetails; }

    public List<AppliedPromoCodeDTO> getAppliedPromoCodes() { return appliedPromoCodes; }

    public Double getTotalDiscount() { return totalDiscount; }

    public Double getFinalAmount() { return finalAmount; }

    public static class AppliedPromoCodeDTO {

        private String promoCode;
        private String discountType;
        private Double discountApplied;
        private LocalDateTime appliedAt;

        private AppliedPromoCodeDTO(Builder builder) {
            this.promoCode = builder.promoCode;
            this.discountType = builder.discountType;
            this.discountApplied = builder.discountApplied;
            this.appliedAt = builder.appliedAt;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String promoCode;
            private String discountType;
            private Double discountApplied;
            private LocalDateTime appliedAt;

            public Builder promoCode(String promoCode) {
                this.promoCode = promoCode;
                return this;
            }

            public Builder discountType(String discountType) {
                this.discountType = discountType;
                return this;
            }

            public Builder discountApplied(Double discountApplied) {
                this.discountApplied = discountApplied;
                return this;
            }

            public Builder appliedAt(LocalDateTime appliedAt) {
                this.appliedAt = appliedAt;
                return this;
            }

            public AppliedPromoCodeDTO build() {
                return new AppliedPromoCodeDTO(this);
            }
        }

        // getters (REQUIRED for serialization)
        public String getPromoCode() { return promoCode; }
        public String getDiscountType() { return discountType; }
        public Double getDiscountApplied() { return discountApplied; }
        public LocalDateTime getAppliedAt() { return appliedAt; }
    }
}