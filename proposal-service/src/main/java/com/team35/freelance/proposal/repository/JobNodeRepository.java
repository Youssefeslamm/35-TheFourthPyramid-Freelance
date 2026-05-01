package com.team35.freelance.proposal.repository;

import com.team35.freelance.proposal.model.JobNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobNodeRepository extends Neo4jRepository<JobNode, Long> {
}