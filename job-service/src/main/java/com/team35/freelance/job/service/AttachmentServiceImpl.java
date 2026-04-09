package com.team35.freelance.job.service;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
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
}
