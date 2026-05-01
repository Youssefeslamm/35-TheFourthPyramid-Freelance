package com.team35.freelance.wallet.common.adapter;

import com.team35.freelance.wallet.dto.PayoutDetailsDTO;
import com.team35.freelance.wallet.dto.PromoCodeUsage;
import com.team35.freelance.wallet.dto.RevenueReportDTO;
import com.team35.freelance.wallet.model.PayoutMethod;
import com.team35.freelance.wallet.model.PayoutStatus;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public class MongoDocumentAdapter {

    @SuppressWarnings("unchecked")
    public <T> T adapt(Document mongoDocument, Class<T> targetType) {
        if (mongoDocument == null) {
            return null;
        }

        if (targetType == PayoutDetailsDTO.class) {
            PayoutDetailsDTO.Builder builder = PayoutDetailsDTO.builder();

            Number payoutId = (Number) mongoDocument.get("payoutId");
            if (payoutId != null) {
                builder.payoutId(payoutId.longValue());
            }

            Number contractId = (Number) mongoDocument.get("contractId");
            if (contractId != null) {
                builder.contractId(contractId.longValue());
            }

            Number freelancerId = (Number) mongoDocument.get("freelancerId");
            if (freelancerId != null) {
                builder.freelancerId(freelancerId.longValue());
            }

            Double originalAmount = mongoDocument.getDouble("originalAmount");
            if (originalAmount != null) {
                builder.originalAmount(originalAmount);
            }

            String method = mongoDocument.getString("method");
            if (method != null) {
                builder.method(PayoutMethod.valueOf(method));
            }

            String status = mongoDocument.getString("status");
            if (status != null) {
                builder.status(PayoutStatus.valueOf(status));
            }

            Object details = mongoDocument.get("transactionDetails");
            if (details instanceof Map<?, ?>) {
                builder.transactionDetails((Map<String, Object>) details);
            }

            Object promos = mongoDocument.get("appliedPromoCodes");
            if (promos instanceof List<?>) {
                builder.appliedPromoCodes((List<PayoutDetailsDTO.AppliedPromoCodeDTO>) promos);
            }

            Double totalDiscount = mongoDocument.getDouble("totalDiscount");
            if (totalDiscount != null) {
                builder.totalDiscount(totalDiscount);
            }

            Double finalAmount = mongoDocument.getDouble("finalAmount");
            if (finalAmount != null) {
                builder.finalAmount(finalAmount);
            }

            return (T) builder.build();
        }

        if (targetType == PromoCodeUsage.class) {
            PromoCodeUsage.Builder builder = PromoCodeUsage.builder();

            Number promoCodeId = (Number) mongoDocument.get("promoCodeId");
            if (promoCodeId != null) {
                builder.promoCodeId(promoCodeId.longValue());
            }

            String code = mongoDocument.getString("code");
            if (code != null) {
                builder.code(code);
            }

            String discountType = mongoDocument.getString("discountType");
            if (discountType != null) {
                builder.discountType(discountType);
            }

            Double discountValue = mongoDocument.getDouble("discountValue");
            if (discountValue != null) {
                builder.discountValue(discountValue);
            }

            Number timesUsed = (Number) mongoDocument.get("timesUsed");
            if (timesUsed != null) {
                builder.timesUsed(timesUsed.intValue());
            }

            Double totalDiscountGiven = mongoDocument.getDouble("totalDiscountGiven");
            if (totalDiscountGiven != null) {
                builder.totalDiscountGiven(totalDiscountGiven);
            }

            Boolean active = mongoDocument.getBoolean("active");
            if (active != null) {
                builder.active(active);
            }

            Boolean expired = mongoDocument.getBoolean("expired");
            if (expired != null) {
                builder.expired(expired);
            }

            return (T) builder.build();
        }

        if (targetType == RevenueReportDTO.class) {
            RevenueReportDTO.Builder builder = RevenueReportDTO.builder();

            Double totalRevenue = mongoDocument.getDouble("totalRevenue");
            if (totalRevenue != null) {
                builder.totalRevenue(totalRevenue);
            }

            Number totalTransactions = (Number) mongoDocument.get("totalTransactions");
            if (totalTransactions != null) {
                builder.totalTransactions(totalTransactions.longValue());
            }

            Double averagePayout = mongoDocument.getDouble("averagePayout");
            if (averagePayout != null) {
                builder.averagePayout(averagePayout);
            }

            Double refundedAmount = mongoDocument.getDouble("refundedAmount");
            if (refundedAmount != null) {
                builder.refundedAmount(refundedAmount);
            }

            Number refundCount = (Number) mongoDocument.get("refundCount");
            if (refundCount != null) {
                builder.refundCount(refundCount.longValue());
            }

            return (T) builder.build();
        }

        return buildEmpty(targetType);
    }

    private <T> T buildEmpty(Class<T> targetType) {
        try {
            Object builder = targetType.getMethod("builder").invoke(null);
            return (T) builder.getClass().getMethod("build").invoke(builder);
        } catch (NoSuchMethodException ignored) {
            try {
                return targetType.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Unsupported target type: " + targetType.getName(), e);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to build target type: " + targetType.getName(), e);
        }
    }
}
