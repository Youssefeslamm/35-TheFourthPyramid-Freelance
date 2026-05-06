package com.team35.freelance.wallet.dto;

import java.io.Serializable;

public class PromoCodeUsageDTO implements Serializable {

    private Long promoCodeId;
    private String code;
    private String discountType;
    private Double discountValue;
    private Integer timesUsed;
    private Double totalDiscountGiven;
    private Boolean active;
    private Boolean expired;

    private PromoCodeUsageDTO(Builder builder) {
        this.promoCodeId = builder.promoCodeId;
        this.code = builder.code;
        this.discountType = builder.discountType;
        this.discountValue = builder.discountValue;
        this.timesUsed = builder.timesUsed;
        this.totalDiscountGiven = builder.totalDiscountGiven;
        this.active = builder.active;
        this.expired = builder.expired;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long promoCodeId;
        private String code;
        private String discountType;
        private Double discountValue;
        private Integer timesUsed;
        private Double totalDiscountGiven;
        private Boolean active;
        private Boolean expired;

        public Builder promoCodeId(Long promoCodeId) {
            this.promoCodeId = promoCodeId;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder discountType(String discountType) {
            this.discountType = discountType;
            return this;
        }

        public Builder discountValue(Double discountValue) {
            this.discountValue = discountValue;
            return this;
        }

        public Builder timesUsed(Integer timesUsed) {
            this.timesUsed = timesUsed;
            return this;
        }

        public Builder totalDiscountGiven(Double totalDiscountGiven) {
            this.totalDiscountGiven = totalDiscountGiven;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder expired(Boolean expired) {
            this.expired = expired;
            return this;
        }

        public PromoCodeUsageDTO build() {
            return new PromoCodeUsageDTO(this);
        }
    }

    public Long getPromoCodeId() { return promoCodeId; }
    public String getCode() { return code; }
    public String getDiscountType() { return discountType; }
    public Double getDiscountValue() { return discountValue; }
    public Integer getTimesUsed() { return timesUsed; }
    public Double getTotalDiscountGiven() { return totalDiscountGiven; }
    public Boolean getActive() { return active; }
    public Boolean getExpired() { return expired; }
}
