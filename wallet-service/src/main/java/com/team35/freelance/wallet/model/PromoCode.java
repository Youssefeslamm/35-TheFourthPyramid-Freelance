package com.team35.freelance.wallet.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "promo_codes")
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, columnDefinition = "discount_type_enum")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private DiscountType discountType;

    @Column(nullable = false)
    private Double discountValue;

    @Column(nullable = false)
    private Integer maxUses;

    @Column(nullable = false)
    private Integer currentUses = 0;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean active = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @OneToMany(mappedBy = "promoCode", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PayoutPromo> payoutPromos = new ArrayList<>();

    public PromoCode() {
    }

    public PromoCode(Long id,
                     String code,
                     DiscountType discountType,
                     Double discountValue,
                     Integer maxUses,
                     Integer currentUses,
                     LocalDateTime expiryDate,
                     Boolean active,
                     Map<String, Object> metadata,
                     List<PayoutPromo> payoutPromos
    ) {
        this.id = id;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxUses = maxUses;
        this.currentUses = currentUses;
        this.expiryDate = expiryDate;
        this.active = active;
        this.metadata = metadata;
        this.payoutPromos = payoutPromos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getCurrentUses() {
        return currentUses;
    }

    public void setCurrentUses(Integer currentUses) {
        this.currentUses = currentUses;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<PayoutPromo> getPayoutPromos() {
        return payoutPromos;
    }

    public void setPayoutPromos(List<PayoutPromo> payoutPromos) {
        this.payoutPromos = payoutPromos;
    }
}