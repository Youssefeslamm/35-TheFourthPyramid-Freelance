package com.team35.freelance.proposal.repository;

import com.team35.freelance.proposal.model.Proposal;
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

    // S3-F1
    @Query(value = """
    SELECT * FROM proposals
    WHERE submitted_at BETWEEN :startDate AND :endDate
    AND (:status IS NULL OR status = :status)
    ORDER BY submitted_at DESC
    """, nativeQuery = true)
    List<Proposal> findByStatusAndDateRange(
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}