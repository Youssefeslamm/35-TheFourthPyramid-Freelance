package com.team35.freelance.job.repository;
import com.team35.freelance.job.model.JobAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.team35.freelance.job.model.AttachmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface AttachmentRepository extends JpaRepository<JobAttachment, Long> {

    // 1. Custom Finder: Find all attachments for a specific job
    List<JobAttachment> findByJobId(Long jobId);

    // 2. Pagination: Essential for scalability (returning only small chunks of data)
    Page<JobAttachment> findByJobId(Long jobId, Pageable pageable);

    // 3. Complex Query: Find verified attachments of a specific type
    @Query("SELECT a FROM JobAttachment a WHERE a.jobId = :jobId AND a.type = :type AND a.verified = true")
    List<JobAttachment> findVerifiedByType(@Param("jobId") Long jobId, @Param("type") AttachmentType type);
}