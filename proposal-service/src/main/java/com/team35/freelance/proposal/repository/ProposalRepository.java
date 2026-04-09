package com.team35.freelance.proposal.repository;
import com.team35.freelance.proposal.model.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    @Query(value = """
    SELECT COUNT(*) FROM proposals
    WHERE status IN ('SUBMITTED', 'SHORTLISTED')
    AND bid_amount BETWEEN :minBid AND :maxBid
    """, nativeQuery = true)
    long countSimilarActiveProposals(@Param("minBid") double minBid,
                                     @Param("maxBid") double maxBid);

    @Query(value = """
    SELECT COUNT(*) FROM proposals
    WHERE job_id = :jobId
    AND id != :excludeId
    AND status IN ('SUBMITTED', 'SHORTLISTED', 'ACCEPTED')
    """, nativeQuery = true)
    long countOtherActiveProposalsForJob(@Param("jobId") long jobId,
                                         @Param("excludeId") long excludeId);


    @Modifying
    @Transactional
    @Query(value = "UPDATE jobs SET status = 'OPEN' WHERE id = :jobId",
            nativeQuery = true)
    void revertJobToOpen(@Param("jobId") long jobId);

}

