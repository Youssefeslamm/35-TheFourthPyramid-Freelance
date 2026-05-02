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

    @Modifying
    @Transactional
    @Query(value = "UPDATE jobs SET status = 'OPEN' WHERE id = :jobId", nativeQuery = true)
    void revertJobToOpen(@Param("jobId") long jobId);

    // S3-F2: Get freelancer role
    @Query(value = "SELECT role FROM users WHERE id = :freelancerId", nativeQuery = true)
    String findFreelancerRole(@Param("freelancerId") Long freelancerId);

    // S3-F2: Update job to IN_PROGRESS
    @Modifying
    @Transactional
    @Query(value = "UPDATE jobs SET status = 'IN_PROGRESS' WHERE id = :jobId", nativeQuery = true)
    void updateJobStatusToInProgress(@Param("jobId") Long jobId);

    // S3-F2: Get clientId from job
    @Query(value = "SELECT client_id FROM jobs WHERE id = :jobId", nativeQuery = true)
    Long findClientIdByJobId(@Param("jobId") Long jobId);

    // S3-F2: Insert new contract
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO contracts (job_id, freelancer_id, client_id, proposal_id, agreed_amount, status, start_date, created_at, metadata) " +
            "VALUES (:jobId, :freelancerId, :clientId, :proposalId, :agreedAmount, 'ACTIVE', NOW(), NOW(), '{}'::jsonb)",
            nativeQuery = true)
    void insertContract(@Param("jobId") Long jobId,
                        @Param("freelancerId") Long freelancerId,
                        @Param("clientId") Long clientId,
                        @Param("proposalId") Long proposalId,
                        @Param("agreedAmount") Double agreedAmount);
    // S3-F4: Find active contract for proposal
    @Query(value = "SELECT id FROM contracts WHERE proposal_id = :proposalId AND status = 'ACTIVE' LIMIT 1",
            nativeQuery = true)
    Long findActiveContractIdByProposalId(@Param("proposalId") Long proposalId);

    // S3-F4: Get agreed amount from contract
    @Query(value = "SELECT agreed_amount FROM contracts WHERE id = :contractId", nativeQuery = true)
    Double findAgreedAmountByContractId(@Param("contractId") Long contractId);

    // S3-F4: Get job_id from contract
    @Query(value = "SELECT job_id FROM contracts WHERE id = :contractId", nativeQuery = true)
    Long findJobIdByContractId(@Param("contractId") Long contractId);

    // S3-F4: Get freelancer_id from contract
    @Query(value = "SELECT freelancer_id FROM contracts WHERE id = :contractId", nativeQuery = true)
    Long findFreelancerIdByContractId(@Param("contractId") Long contractId);

    // S3-F4: Complete contract
    @Modifying
    @Transactional
    @Query(value = "UPDATE contracts SET status = 'COMPLETED', end_date = NOW() WHERE id = :contractId",
            nativeQuery = true)
    void completeContract(@Param("contractId") Long contractId);

    // S3-F4: Close job
    @Modifying
    @Transactional
    @Query(value = "UPDATE jobs SET status = 'CLOSED' WHERE id = :jobId", nativeQuery = true)
    void updateJobStatusToClosed(@Param("jobId") Long jobId);

    // S3-F4: Insert pending payout
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO payouts (contract_id, freelancer_id, amount, method, status, created_at, transaction_details) " +
            "VALUES (:contractId, :freelancerId, :amount, 'BANK_TRANSFER', 'PENDING', NOW(), '{}'::jsonb)",
            nativeQuery = true)
    void insertPendingPayout(@Param("contractId") Long contractId,
                             @Param("freelancerId") Long freelancerId,
                             @Param("amount") Double amount);
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
    // S3-F11: Get freelancer name
    @Query(value = "SELECT name FROM users WHERE id = :freelancerId", nativeQuery = true)
    String findFreelancerNameById(@Param("freelancerId") Long freelancerId);

    // S3-F11: Get job title and category
    @Query(value = "SELECT title || '|' || category FROM jobs WHERE id = :jobId", nativeQuery = true)
    String findJobTitleAndCategoryById(@Param("jobId") Long jobId);
    //S3-F12
    @Query(value = "SELECT name FROM users WHERE id = :userId", nativeQuery = true)
    String findUserNameById(@Param("userId") Long userId);

    @Query(value = "SELECT title FROM jobs WHERE id = :jobId", nativeQuery = true)
    String findJobTitleById(@Param("jobId") Long jobId);

    @Query(value = "SELECT category FROM jobs WHERE id = :jobId", nativeQuery = true)
    String findJobCategoryById(@Param("jobId") Long jobId);

    @Query(value = "SELECT id, title, category FROM jobs WHERE id IN :jobIds", nativeQuery = true)
    List<Object[]> findJobDetailsByIds(@Param("jobIds") List<Long> jobIds);

}