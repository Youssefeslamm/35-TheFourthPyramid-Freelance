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

    List<JobAttachment> findByJobId(Long jobId);

  
    Page<JobAttachment> findByJobId(Long jobId, Pageable pageable);

   
    @Query("SELECT a FROM JobAttachment a WHERE a.jobId = :jobId AND a.type = :type AND a.verified = true")
    List<JobAttachment> findVerifiedByType(@Param("jobId") Long jobId, @Param("type") AttachmentType type);
}