package com.team35.freelance.contract.cassandra;

import org.springframework.data.cassandra.repository.MapIdCassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractMilestoneEventRepository extends MapIdCassandraRepository<ContractMilestoneEvent> {
}
