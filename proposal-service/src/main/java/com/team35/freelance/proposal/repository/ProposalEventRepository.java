package com.team35.freelance.proposal.repository;

import com.team35.freelance.proposal.event.ProposalEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProposalEventRepository extends MongoRepository<ProposalEvent, String> {
}
