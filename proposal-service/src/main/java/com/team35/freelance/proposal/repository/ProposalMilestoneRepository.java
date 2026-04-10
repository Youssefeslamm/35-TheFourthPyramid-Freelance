package com.team35.freelance.proposal.repository;

import com.team35.freelance.proposal.model.ProposalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProposalMilestoneRepository extends JpaRepository<ProposalMilestone, Long> {

    @Query(value = "SELECT COALESCE(MAX(milestone_order), 0) FROM proposal_milestones WHERE proposal_id = :proposalId",
            nativeQuery = true)
    Integer findMaxMilestoneOrderByProposalId(@Param("proposalId") Long proposalId);

    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM proposal_milestones WHERE proposal_id = :proposalId",
            nativeQuery = true)
    Double findTotalMilestoneAmountByProposalId(@Param("proposalId") Long proposalId);

    List<ProposalMilestone> findByProposalIdOrderByMilestoneOrderAsc(Long proposalId);
}