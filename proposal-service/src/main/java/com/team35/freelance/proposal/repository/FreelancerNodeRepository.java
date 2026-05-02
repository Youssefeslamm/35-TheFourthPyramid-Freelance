package com.team35.freelance.proposal.repository;

import com.team35.freelance.proposal.model.neo4j.FreelancerNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FreelancerNodeRepository extends Neo4jRepository<FreelancerNode, Long> {
}