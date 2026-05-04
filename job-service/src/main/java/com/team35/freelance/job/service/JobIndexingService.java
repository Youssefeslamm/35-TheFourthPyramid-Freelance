package com.team35.freelance.job.service;

import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobSearchDocument;
import com.team35.freelance.job.repository.JobRepository;
import com.team35.freelance.job.repository.JobSearchRepository;
import org.springframework.stereotype.Service;

@Service
public class JobIndexingService {

    private final JobRepository jobRepository;
    private final JobSearchRepository jobSearchRepository;

    public JobIndexingService(JobRepository jobRepository, JobSearchRepository jobSearchRepository) {
        this.jobRepository = jobRepository;
        this.jobSearchRepository = jobSearchRepository;
    }

    public void indexJob(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            JobSearchDocument doc = new JobSearchDocument();
            doc.setId(job.getId().toString());
            doc.setTitle(job.getTitle());
            doc.setDescription(job.getDescription());
            // Convert Enums to Strings using .name()
            doc.setCategory(job.getCategory() != null ? job.getCategory().name() : null);
            doc.setStatus(job.getStatus() != null ? job.getStatus().name() : null);
            doc.setBudgetMin(job.getBudgetMin());
            doc.setBudgetMax(job.getBudgetMax());

            jobSearchRepository.save(doc);
        });
    }

    public void removeJobFromIndex(Long jobId) {
        jobSearchRepository.deleteById(jobId.toString());
    }
}