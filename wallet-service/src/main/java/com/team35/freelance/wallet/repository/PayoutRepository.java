package com.team35.freelance.wallet.repository;

import com.team35.freelance.wallet.model.Payout;
import com.team35.freelance.wallet.model.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    // ---------- S5-F3: FREELANCER PAYOUT SUMMARY BY METHOD ----------
    @Query(value = """
        SELECT
            COUNT(*) AS total_payouts,
            COALESCE(SUM(p.amount), 0) AS total_amount
        FROM payouts p
        WHERE p.freelancer_id = :freelancerId
          AND p.status = 'COMPLETED'
    """, nativeQuery = true)
    Object[] getFreelancerBasicSummary(@Param("freelancerId") Long freelancerId);

    @Query(value = """
    SELECT
        p.method,
        COUNT(*) AS method_count,
        COALESCE(SUM(p.amount), 0) AS method_total
    FROM payouts p
    WHERE p.freelancer_id = :freelancerId
      AND p.status = 'COMPLETED'
    GROUP BY p.method
""", nativeQuery = true)
    List<Object[]> getFreelancerMethodBreakdown(@Param("freelancerId") Long freelancerId);

    // ---------- EXISTING METHODS ----------
    Payout findByContractId(Long contractId);

    Payout findFirstByContractIdAndStatusOrderByCreatedAtDesc(Long contractId, PayoutStatus status);

    List<Payout> findByFreelancerIdAndStatusIn(
            Long freelancerId,
            List<PayoutStatus> statuses
    );

    List<Payout> findByStatusOrderByCreatedAtDesc(PayoutStatus status);

    List<Payout> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    List<Payout> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            PayoutStatus status,
            LocalDateTime start,
            LocalDateTime end
    );


    // ---------- S5-F6: REVENUE REPORT ----------
    @Query(value = """
    SELECT
        COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END), 0) AS totalRevenue,
        COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 END) AS totalTransactions,
        COALESCE(SUM(CASE WHEN p.status = 'REFUNDED' THEN p.amount ELSE 0 END), 0) AS refundedAmount,
        COUNT(CASE WHEN p.status = 'REFUNDED' THEN 1 END) AS refundCount
    FROM payouts p
    WHERE p.created_at BETWEEN :startDate AND :endDate
""", nativeQuery = true)
    List<Object[]> getRevenueReport(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
    SELECT
        p.method,
        SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END) AS successCount,
        SUM(CASE WHEN p.status = 'FAILED' THEN 1 ELSE 0 END) AS failureCount,
        COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END), 0) AS totalAmount
    FROM payouts p
    WHERE p.created_at BETWEEN :startDateTime AND :endDateTime
      AND p.method IS NOT NULL
      AND p.status IN ('COMPLETED', 'FAILED')
    GROUP BY p.method
    """, nativeQuery = true)
    List<Object[]> getPayoutMethodBreakdown(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );



}
