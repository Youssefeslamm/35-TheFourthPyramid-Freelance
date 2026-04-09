package com.team35.freelance.wallet.repository;

import com.team35.freelance.wallet.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCode(String code);
    boolean existsByCode(String code);

    @Query(value = """
            SELECT 
                pc.id AS promoCodeId,
                pc.code AS code,
                pc.discount_type AS discountType,
                pc.discount_value AS discountValue,
                pc.current_uses AS timesUsed,
                COALESCE(SUM(pp.discount_applied), 0) AS totalDiscountGiven,
                pc.active AS active,
                pc.expiry_date AS expiryDate
            FROM promo_codes pc
            LEFT JOIN payout_promos pp ON pc.id = pp.promo_code_id
            GROUP BY pc.id, pc.code, pc.discount_type, pc.discount_value, pc.current_uses, pc.active, pc.expiry_date
            ORDER BY pc.current_uses DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopUsedPromoCodes(@Param("limit") int limit);
}