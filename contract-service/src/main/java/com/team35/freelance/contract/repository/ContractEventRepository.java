package com.team35.freelance.contract.repository;

import com.team35.freelance.contract.document.ContractEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractEventRepository extends MongoRepository<ContractEvent, String> {
}
