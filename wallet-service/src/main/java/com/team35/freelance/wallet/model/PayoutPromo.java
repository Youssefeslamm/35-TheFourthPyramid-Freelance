package com.team35.freelance.wallet.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_promos")
public class PayoutPromo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double discountApplied;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id", nullable = false)
    @JsonIgnore
    private Payout payout;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id", nullable = false)
    private PromoCode promoCode;

    public PayoutPromo() {
    }

    public PayoutPromo(Double discountApplied, LocalDateTime appliedAt, Payout payout, PromoCode promoCode) {
        this.discountApplied = discountApplied;
        this.appliedAt = appliedAt;
        this.payout = payout;
        this.promoCode = promoCode;
    }

    public Long getId() {
        return id;
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

    public Payout getPayout() {
        return payout;
    }

    public void setPayout(Payout payout) {
        this.payout = payout;
    }

    public PromoCode getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(PromoCode promoCode) {
        this.promoCode = promoCode;
    }
}
