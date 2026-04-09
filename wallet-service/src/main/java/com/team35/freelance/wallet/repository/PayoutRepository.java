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

    // ---------- EXISTING SUMMARY ----------
    @Query(value = """
        SELECT 
            p.freelancer_id,
            COUNT(*) AS total_payouts,
            SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN p.status = 'FAILED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN p.status = 'REFUNDED' THEN 1 ELSE 0 END),
            COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amount ELSE 0 END), 0),
            COALESCE(AVG(CASE WHEN p.status = 'COMPLETED' THEN p.amount END), 0)
        FROM payouts p
        WHERE p.freelancer_id = :freelancerId
        GROUP BY p.freelancer_id
    """, nativeQuery = true)
    Object[] getFreelancerPayoutSummary(@Param("freelancerId") Long freelancerId);

    // ---------- NEW METHODS ----------
    Payout findByContractId(Long contractId);

    @Query(value = "SELECT status FROM contracts WHERE id = :contractId", nativeQuery = true)
    String getContractStatus(@Param("contractId") Long contractId);
    List<Payout> findByStatusOrderByCreatedAtDesc(PayoutStatus status);

    List<Payout> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end
    );

    List<Payout> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            PayoutStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}