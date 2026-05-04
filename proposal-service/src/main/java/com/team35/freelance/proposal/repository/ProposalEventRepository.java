package com.team35.freelance.proposal.repository;
import com.team35.freelance.proposal.common.event.ProposalEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ProposalEventRepository extends MongoRepository<ProposalEvent, String> {
}
