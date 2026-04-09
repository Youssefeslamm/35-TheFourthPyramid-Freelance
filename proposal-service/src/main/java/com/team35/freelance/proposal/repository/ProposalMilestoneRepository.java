package com.team35.freelance.proposal.repository;
import com.team35.freelance.proposal.model.ProposalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
@Repository
public interface ProposalMilestoneRepository extends JpaRepository<ProposalMilestone, Long> {


import com.team35.freelance.proposal.model.ProposalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProposalMilestoneRepository extends JpaRepository<ProposalMilestone, Long> {
}