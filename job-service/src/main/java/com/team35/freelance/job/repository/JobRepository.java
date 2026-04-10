package com.team35.freelance.job.repository;

import com.team35.freelance.job.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Query(value = """
            SELECT *
            FROM jobs j
            WHERE (:status IS NULL OR j.status = :status)
              AND j.budget_max BETWEEN :minBudget AND :maxBudget
            ORDER BY j.budget_max DESC
            """, nativeQuery = true)
    List<Job> searchJobs(@Param("status") String status,
                         @Param("minBudget") Double minBudget,
                         @Param("maxBudget") Double maxBudget);

    @Query(value = """
    SELECT 
        j.id as jobId, 
        j.title as title, 
        COUNT(p.id) as totalProposals, 
        COALESCE(AVG(p.bid_amount), 0) as averageBidAmount, 
        COALESCE(MIN(p.bid_amount), 0) as lowestBid, 
        COALESCE(MAX(p.bid_amount), 0) as highestBid
    FROM jobs j
    LEFT JOIN proposals p ON j.id = p.job_id 
    AND p.created_at BETWEEN CAST(:startDate AS TIMESTAMP) AND CAST(:endDate AS TIMESTAMP)
    WHERE j.id = :id
    GROUP BY j.id, j.title
    """, nativeQuery = true)
    Map<String, Object> getProposalSummaryRaw(
            @Param("id") Long id,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );}