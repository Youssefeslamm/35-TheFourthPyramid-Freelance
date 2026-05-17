package com.team35.freelance.proposal.repository;

import com.team35.freelance.proposal.model.Proposal;
import com.team35.freelance.proposal.model.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    @Query(value = """

            SELECT COUNT(*) FROM proposals
    WHERE status IN ('SUBMITTED', 'SHORTLISTED')
    AND bid_amount BETWEEN :minBid AND :maxBid
    """, nativeQuery = true)
    long countSimilarActiveProposals(@Param("minBid") double minBid,
                                     @Param("maxBid") double maxBid);

    @Query(value = """
    SELECT COUNT(*) FROM proposals
    WHERE job_id = :jobId
    AND id != :excludeId
    AND status IN ('SUBMITTED', 'SHORTLISTED', 'ACCEPTED')
    """, nativeQuery = true)
    long countOtherActiveProposalsForJob(@Param("jobId") long jobId,
                                         @Param("excludeId") long excludeId);

    // S3-F6
    @Query(value = """
    SELECT
        COUNT(*) AS totalProposals,
        COALESCE(SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END), 0),
        COALESCE(SUM(bid_amount), 0),
        COALESCE(AVG(bid_amount), 0),
        COALESCE(
            ROUND(
                100.0 * SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END)
                / NULLIF(COUNT(*), 0),
            2),
        0)
    FROM proposals
    WHERE submitted_at BETWEEN :startDate AND :endDate
    """, nativeQuery = true)

    List<Object[]> getAnalytics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // S3-F5
    @Query(value = """
    SELECT * FROM proposals
    WHERE metadata ->> :key = :value
    """, nativeQuery = true)
    List<Proposal> findByMetadataField(
            @Param("key") String key,
            @Param("value") String value);

    // S3-F1
    @Query(value = """
    SELECT * FROM proposals
    WHERE submitted_at BETWEEN :startDate AND :endDate
    AND (CAST(:status AS TEXT) IS NULL OR status = CAST(:status AS proposal_status_enum))
    ORDER BY submitted_at DESC
    """, nativeQuery = true)
    List<Proposal> findByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
    SELECT * FROM proposals p
    WHERE (CAST(:status AS TEXT) IS NULL OR p.status = CAST(:status AS proposal_status_enum))
      AND (CAST(:freelancerId AS BIGINT) IS NULL OR p.freelancer_id = :freelancerId)
      AND (CAST(:jobId AS BIGINT) IS NULL OR p.job_id = :jobId)
      AND (CAST(:minBid AS DOUBLE PRECISION) IS NULL OR p.bid_amount >= :minBid)
      AND (CAST(:maxBid AS DOUBLE PRECISION) IS NULL OR p.bid_amount <= :maxBid)
      AND (CAST(:startDate AS TIMESTAMP) IS NULL OR p.submitted_at >= :startDate)
      AND (CAST(:endDate AS TIMESTAMP) IS NULL OR p.submitted_at <= :endDate)
    ORDER BY p.submitted_at DESC
    """, nativeQuery = true)
    List<Proposal> searchProposals(
            @Param("status") String status,
            @Param("freelancerId") Long freelancerId,
            @Param("jobId") Long jobId,
            @Param("minBid") Double minBid,
            @Param("maxBid") Double maxBid,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    //S3-F10
    @Query(value = """
    SELECT 
        COUNT(*) as totalProposals,
        AVG(bid_amount) as averageBidAmount,
        AVG(estimated_days) as averageEstimatedDays,
        SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END) as acceptedCount,
        SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) as rejectedCount,
        SUM(CASE WHEN status = 'WITHDRAWN' THEN 1 ELSE 0 END) as withdrawnCount,
        SUM(CASE WHEN status = 'SUBMITTED' THEN 1 ELSE 0 END) as submittedCount,
        SUM(CASE WHEN status = 'SHORTLISTED' THEN 1 ELSE 0 END) as shortlistedCount
    FROM proposals
    WHERE submitted_at BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
    Object[] getProposalAnalytics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
    SELECT
        COUNT(*) as totalProposals,
        COALESCE(AVG(bid_amount), 0) as averageBidAmount,
        COALESCE(AVG(estimated_days), 0) as averageEstimatedDays,
        COALESCE(SUM(CASE WHEN status = 'ACCEPTED' THEN 1 ELSE 0 END), 0) as acceptedCount,
        COALESCE(SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END), 0) as rejectedCount,
        COALESCE(SUM(CASE WHEN status = 'WITHDRAWN' THEN 1 ELSE 0 END), 0) as withdrawnCount,
        COALESCE(SUM(CASE WHEN status = 'SUBMITTED' THEN 1 ELSE 0 END), 0) as submittedCount,
        COALESCE(SUM(CASE WHEN status = 'SHORTLISTED' THEN 1 ELSE 0 END), 0) as shortlistedCount
    FROM proposals p
    WHERE (CAST(:startDate AS TIMESTAMP) IS NULL OR p.submitted_at >= :startDate)
      AND (CAST(:endDate AS TIMESTAMP) IS NULL OR p.submitted_at <= :endDate)
      AND (CAST(:freelancerId AS BIGINT) IS NULL OR p.freelancer_id = :freelancerId)
      AND (CAST(:jobId AS BIGINT) IS NULL OR p.job_id = :jobId)
      AND (CAST(:status AS TEXT) IS NULL OR p.status = CAST(:status AS proposal_status_enum))
    """, nativeQuery = true)
    Object[] getProposalAnalyticsFiltered(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("freelancerId") Long freelancerId,
            @Param("jobId") Long jobId,
            @Param("status") String status
    );
    // Saga abandonment reaper — finds proposals stuck in PAYMENT_PENDING past the cutoff
    List<Proposal> findByStatusAndAcceptedAtBefore(ProposalStatus status, LocalDateTime cutoff);
}
