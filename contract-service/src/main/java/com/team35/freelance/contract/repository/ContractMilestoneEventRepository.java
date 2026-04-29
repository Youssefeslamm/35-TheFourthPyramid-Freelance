package com.team35.freelance.contract.repository;

import com.team35.freelance.contract.cassandra.ContractMilestoneEvent;
import com.team35.freelance.contract.cassandra.ContractMilestoneEventKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractMilestoneEventRepository extends CassandraRepository<ContractMilestoneEvent, ContractMilestoneEventKey> {
}
