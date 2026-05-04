package com.team35.freelance.job.repository;

import com.team35.freelance.job.elasticsearch.JobSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobSearchRepository extends ElasticsearchRepository<JobSearchDocument, Long> {
}
