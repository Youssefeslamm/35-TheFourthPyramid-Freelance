package com.team35.freelance.job.repository;
import com.team35.freelance.job.model.JobAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<JobAttachment, Long> {
}