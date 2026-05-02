package com.team35.freelance.contract.repository;

import com.team35.freelance.contract.event.ContractEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ContractEventRepository extends MongoRepository<ContractEvent, String> {
}