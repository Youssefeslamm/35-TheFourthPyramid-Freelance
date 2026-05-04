package com.team35.freelance.wallet.dto;

import java.io.Serializable;

public class PromoCodeUsage implements Serializable {

    private Long promoCodeId;
    private String code;
    private String discountType;
    private Double discountValue;
    private Integer timesUsed;
    private Double totalDiscountGiven;
    private Boolean active;
    private Boolean expired;

    private PromoCodeUsage(Builder builder) {
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

        public Builder promoCodeId(Long val) { this.promoCodeId = val; return this; }
        public Builder code(String val) { this.code = val; return this; }
        public Builder discountType(String val) { this.discountType = val; return this; }
        public Builder discountValue(Double val) { this.discountValue = val; return this; }
        public Builder timesUsed(Integer val) { this.timesUsed = val; return this; }
        public Builder totalDiscountGiven(Double val) { this.totalDiscountGiven = val; return this; }
        public Builder active(Boolean val) { this.active = val; return this; }
        public Builder expired(Boolean val) { this.expired = val; return this; }

        public PromoCodeUsage build() {
            return new PromoCodeUsage(this);
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