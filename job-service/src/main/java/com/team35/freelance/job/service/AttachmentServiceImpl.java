package com.team35.freelance.job.service;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;  // Fixes the Page error
import com.team35.freelance.job.model.AttachmentType; // Fixes the red AttachmentType
@Service
public class AttachmentServiceImpl implements AttachmentService {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Override
    public JobAttachment saveAttachment(JobAttachment attachment) {
        return attachmentRepository.save(attachment);
    }

    public List<JobAttachment> getAttachmentsByJob(Long jobId) {
        // We'll implement custom queries in Phase C, for now just a simple find
        return attachmentRepository.findAll();
    }
    @Override
    public Page<JobAttachment> getAttachmentsPaged(Long jobId, int page, int size) {
        return attachmentRepository.findByJobId(jobId, PageRequest.of(page, size));
    }

    @Override
    public List<JobAttachment> getVerifiedAttachments(Long jobId, AttachmentType type) {
        return attachmentRepository.findVerifiedByType(jobId, type);
    }
}
