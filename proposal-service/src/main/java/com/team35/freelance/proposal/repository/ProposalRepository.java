package com.team35.freelance.proposal.repository;

import com.team35.freelance.proposal.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
}