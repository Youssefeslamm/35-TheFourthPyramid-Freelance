package com.team35.freelance.job.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.team35.freelance.job.dto.ContractLookupProjection;
import com.team35.freelance.job.dto.ExpiredJobProjection;
import com.team35.freelance.job.model.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // --- S2-F1: Multi-parameter Search ---
    @Query(value = """
        SELECT *
        FROM jobs j
        WHERE (:status IS NULL OR j.status = CAST(:status AS job_status_enum))
          AND j.budget_max BETWEEN :minBudget AND :maxBudget
        ORDER BY j.budget_max DESC
        """, nativeQuery = true)
    List<Job> searchJobs(@Param("status") String status,
                         @Param("minBudget") Double minBudget,
                         @Param("maxBudget") Double maxBudget);

    // --- S2-F3: Proposal Summary Aggregation ---
    @Query(value = """
    SELECT 
        j.id as jobid, 
        j.title as title, 
        COUNT(p.id) as totalproposals, 
        COALESCE(AVG(p.bid_amount), 0) as averagebidamount, 
        COALESCE(MIN(p.bid_amount), 0) as lowestbid, 
        COALESCE(MAX(p.bid_amount), 0) as highestbid
    FROM jobs j
    LEFT JOIN proposals p ON j.id = p.job_id 
    AND p.submitted_at BETWEEN CAST(:startDate AS TIMESTAMP) AND CAST(:endDate AS TIMESTAMP) 
    WHERE j.id = :id
    GROUP BY j.id, j.title
    """, nativeQuery = true)
    Map<String, Object> getProposalSummaryRaw(
            @Param("id") Long id,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    // --- S2-F9: Identify Jobs with Expired Attachments ---
    @Query(value = """
            SELECT DISTINCT j.id AS jobId
            FROM jobs j
            JOIN job_attachments ja ON ja.job_id = j.id
            WHERE ja.expiry_date < CURRENT_DATE
            ORDER BY j.id
            """, nativeQuery = true)
    List<ExpiredJobProjection> findJobsWithExpiredAttachments();

    @EntityGraph(attributePaths = "jobAttachments")
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    Optional<Job> findByIdWithAttachments(@Param("id") Long id);

    // --- S2-F5: JSONB Requirement Filtering ---
    @Query(value = """
        SELECT *
        FROM jobs j
        WHERE (j.requirements ->> :key) = :value
          AND (:status IS NULL OR j.status = CAST(:status AS job_status_enum))
        ORDER BY j.budget_max DESC
        """, nativeQuery = true)
    List<Job> findByRequirementAndOptionalStatus(@Param("key") String key,
                                                 @Param("value") String value,
                                                 @Param("status") String status);

    // --- S2-F6: Top Budget Report ---
    @Query(value = """
            SELECT j.id,
                   j.title,
                   j.budget_max,
                   COUNT(p.id) AS total_proposals
            FROM jobs j
            LEFT JOIN proposals p ON p.job_id = j.id
            GROUP BY j.id, j.title, j.budget_max
            ORDER BY j.budget_max DESC
            LIMIT :limitValue
            """, nativeQuery = true)
    List<Object[]> findTopBudgetJobs(@Param("limitValue") int limitValue);

    // --- Workflow Helpers (Rating/Closing) ---
    @Query(value = """
            SELECT c.id AS id, c.job_id AS jobId, c.status AS status
            FROM contracts c WHERE c.id = :contractId
            """, nativeQuery = true)
    Optional<ContractLookupProjection> findContractById(@Param("contractId") Long contractId);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END FROM contracts WHERE job_id = :jobId AND status = 'ACTIVE'", nativeQuery = true)
    boolean existsActiveContractForJob(@Param("jobId") Long jobId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE proposals SET status = 'REJECTED' WHERE job_id = :jobId AND status = 'SUBMITTED'", nativeQuery = true)
    int rejectSubmittedProposalsForJob(@Param("jobId") Long jobId);

    // --- S2-F12: Dashboard Aggregation ---
    @Query(value = """
        SELECT
            j.id AS job_id,
            j.title AS title,
            COUNT(p.id) AS total_proposals,
            COALESCE(SUM(CASE WHEN p.status = 'ACCEPTED' THEN 1 ELSE 0 END), 0) AS accepted_proposals,
            COALESCE(AVG(p.bid_amount), 0) AS average_bid_amount,
            (SELECT COUNT(*) FROM job_attachments ja WHERE ja.job_id = j.id AND ja.expiry_date >= CURRENT_DATE) AS active_attachments,
            COALESCE(j.rating, 0) AS rating
        FROM jobs j
        LEFT JOIN proposals p ON p.job_id = j.id
        WHERE j.id = :jobId
        GROUP BY j.id, j.title, j.rating
        """, nativeQuery = true)
    Map<String, Object> getJobDashboardRaw(@Param("jobId") Long jobId);

    @Query(value = """
            SELECT u.role
            FROM users u
            WHERE u.id = :userId
            """, nativeQuery = true)
    Optional<String> findUserRoleById(@Param("userId") Long userId);
}