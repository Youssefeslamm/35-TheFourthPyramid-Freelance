package com.team35.freelance.job.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.team35.freelance.job.dto.ExpiredJobProjection;
import com.team35.freelance.job.model.Job;


@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

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




    @Query(value = """
            SELECT j.id,
                   j.title,
                   j.budget_max,
                   0 AS total_proposals
            FROM jobs j
            ORDER BY j.budget_max DESC
            LIMIT :limitValue
            """, nativeQuery = true)
    List<Object[]> findTopBudgetJobs(@Param("limitValue") int limitValue);

    @Query(value = """
            SELECT *
            FROM jobs j
            WHERE (LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:category IS NULL OR j.category = CAST(:category AS job_category_enum))
              AND (:status IS NULL OR j.status = CAST(:status AS job_status_enum))
              AND (:minBudget IS NULL OR j.budget_max >= :minBudget)
              AND (:maxBudget IS NULL OR j.budget_min <= :maxBudget)
            ORDER BY
              CASE
                WHEN LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0
                ELSE 1
              END,
              j.rating DESC,
              j.created_at DESC
            """, nativeQuery = true)
    List<Job> searchJobsFullTextFallback(@Param("query") String query,
                                         @Param("category") String category,
                                         @Param("status") String status,
                                         @Param("minBudget") Double minBudget,
                                         @Param("maxBudget") Double maxBudget);

}
