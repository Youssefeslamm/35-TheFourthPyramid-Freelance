package com.team35.freelance.wallet.dto;

import java.io.Serializable;

public class CategoryRevenueDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String category;
    private Double netPayoutRevenue;
    private Double platformFeeRevenue;
    private Double totalRevenue;
    private Long payoutCount;

    private CategoryRevenueDTO(Builder builder) {
        this.category = builder.category;
        this.netPayoutRevenue = builder.netPayoutRevenue;
        this.platformFeeRevenue = builder.platformFeeRevenue;
        this.totalRevenue = builder.totalRevenue;
        this.payoutCount = builder.payoutCount;
    }

    public String getCategory() { return category; }
    public Double getNetPayoutRevenue() { return netPayoutRevenue; }
    public Double getPlatformFeeRevenue() { return platformFeeRevenue; }
    public Double getTotalRevenue() { return totalRevenue; }
    public Long getPayoutCount() { return payoutCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String category;
        private Double netPayoutRevenue;
        private Double platformFeeRevenue;
        private Double totalRevenue;
        private Long payoutCount;

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder netPayoutRevenue(Double netPayoutRevenue) {
            this.netPayoutRevenue = netPayoutRevenue;
            return this;
        }

        public Builder platformFeeRevenue(Double platformFeeRevenue) {
            this.platformFeeRevenue = platformFeeRevenue;
            return this;
        }

        public Builder totalRevenue(Double totalRevenue) {
            this.totalRevenue = totalRevenue;
            return this;
        }

        public Builder payoutCount(Long payoutCount) {
            this.payoutCount = payoutCount;
            return this;
        }

        public CategoryRevenueDTO build() {
            return new CategoryRevenueDTO(this);
        }
    }
}