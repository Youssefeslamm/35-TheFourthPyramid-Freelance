package com.team35.freelance.job.repository;

import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobAttachmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobAttachmentRepository extends JpaRepository<JobAttachment, Long> {

    List<JobAttachment> findByJobId(Long jobId);

    Page<JobAttachment> findByJobId(Long jobId, Pageable pageable);

    Optional<JobAttachment> findByIdAndJobId(Long id, Long jobId);

    @Query("""
            SELECT a
            FROM JobAttachment a
            WHERE a.job.id = :jobId
              AND a.type = :type
              AND a.verified = true
            """)
    List<JobAttachment> findVerifiedByType(@Param("jobId") Long jobId,
                                           @Param("type") JobAttachmentType type);
}