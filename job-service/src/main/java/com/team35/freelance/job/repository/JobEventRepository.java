package com.team35.freelance.job.repository;

import com.team35.freelance.job.event.JobEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobEventRepository extends MongoRepository<JobEvent, String> {
}