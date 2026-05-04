package com.team35.freelance.job.repository;


import com.team35.freelance.job.model.MongoEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MongoEventRepository extends MongoRepository<MongoEvent, String> {
}