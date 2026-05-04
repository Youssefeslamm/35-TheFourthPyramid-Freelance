package com.team35.freelance.proposal.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.team35.freelance.proposal.model.neo4j.JobNode;

@Repository
public interface JobNodeRepository extends Neo4jRepository<JobNode, Long> {
    Optional<JobNode> findByJobId(Long jobId);
}