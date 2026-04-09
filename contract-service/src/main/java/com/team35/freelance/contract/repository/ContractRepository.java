package com.team35.freelance.contract.repository;

import com.team35.freelance.contract.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query(nativeQuery = true, value = """
        SELECT 
            c.id AS contractId,
            u.name AS freelancerName,
            j.title AS jobTitle,
            c.agreed_amount AS agreedAmount,
            CAST(c.status AS text) AS status,
            CAST(EXTRACT(EPOCH FROM (COALESCE(c.end_date, CURRENT_TIMESTAMP) - c.start_date)) / 86400 AS INTEGER) AS durationDays
        FROM contracts c
        JOIN users u ON c.freelancer_id = u.id
        JOIN jobs j ON c.job_id = j.id
        WHERE c.agreed_amount >= :minAmount 
          AND c.agreed_amount <= :maxAmount
          AND (:status IS NULL OR CAST(c.status AS text) = :status)
        ORDER BY c.agreed_amount DESC
    """)
    List<Object[]> findContractsByBudgetRangeWithFreelancerInfo(
            @Param("minAmount") Double minAmount, 
            @Param("maxAmount") Double maxAmount, 
            @Param("status") String status
    );
}