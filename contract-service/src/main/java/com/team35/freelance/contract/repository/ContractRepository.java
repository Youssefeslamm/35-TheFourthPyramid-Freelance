package com.team35.freelance.contract.repository;

import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contract.model.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    // S4-EVENTS: find contract by proposalId for completed/cancelled consumers
    Optional<Contract> findByProposalId(Long proposalId);

    @Query(value = "SELECT * FROM contracts WHERE freelancer_id = :userId AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Contract> findMostRecentActiveContractByUserId(@Param("userId") Long userId);

    // --- M3 §6: Feign read APIs (S1 / S2 / S3) ---
    @Query(value = "SELECT CAST(COUNT(*) AS integer) FROM contracts WHERE freelancer_id = :userId AND status = 'ACTIVE'", nativeQuery = true)
    int countActiveByFreelancerId(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM contracts WHERE freelancer_id = :userId AND status = 'COMPLETED'", nativeQuery = true)
    long countCompletedByFreelancerId(@Param("userId") Long userId);

    @Query(value = "SELECT CAST(COUNT(*) AS integer) FROM contracts WHERE job_id = :jobId AND status = 'ACTIVE'", nativeQuery = true)
    int countActiveByJobId(@Param("jobId") Long jobId);

    @Query(value = "SELECT " +
           "CAST(COUNT(*) AS bigint), " +
           "CAST(COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS bigint), " +
           "CAST(COALESCE(SUM(CASE WHEN status = 'TERMINATED' THEN 1 ELSE 0 END), 0) AS bigint), " +
           "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN agreed_amount ELSE 0 END), 0.0), " +
           "COALESCE( " +
           "CASE WHEN COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) > 0 " +
           "THEN SUM(CASE WHEN status = 'COMPLETED' THEN agreed_amount ELSE 0 END) / SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) " +
           "ELSE 0.0 END, " +
           "0.0) " +
           "FROM contracts WHERE freelancer_id = :userId", nativeQuery = true)
    List<Object[]> getUserContractSummaryAggregates(@Param("userId") Long userId);

    Optional<Contract> findFirstByProposalIdAndStatusOrderByCreatedAtDesc(Long proposalId, ContractStatus status);

    @Query(value = "SELECT * FROM contracts WHERE created_at BETWEEN :startDate AND :endDate " +
           "AND (:status IS NULL OR status::text = :status) ORDER BY created_at ASC", nativeQuery = true)
    List<Contract> findContractsInDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("status") String status);

    @Query(value = "SELECT " +
           "COUNT(c.id), " +
           "COALESCE(AVG(c.agreed_amount), 0), " +
           "COALESCE(SUM(CASE WHEN c.status = 'COMPLETED' THEN 1 ELSE 0 END), 0), " +
           "COALESCE(AVG(CASE " +
           "WHEN c.status = 'COMPLETED' AND c.end_date IS NOT NULL " +
           "THEN EXTRACT(EPOCH FROM (c.end_date - c.start_date)) / 86400.0 " +
           "ELSE NULL END), 0) " +
           "FROM contracts c " +
           "WHERE c.start_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Object[]> getContractAnalytics(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT c.status::text, COUNT(c.id) " +
           "FROM contracts c " +
           "WHERE c.start_date BETWEEN :startDate AND :endDate " +
           "GROUP BY c.status", nativeQuery = true)
    List<Object[]> countContractsByStatus(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // S4-F3: returns freelancer_id and job_id so ContractService can enrich via Feign
    @Query(value = "SELECT c.id, c.freelancer_id, c.job_id, c.agreed_amount, c.status, " +
           "EXTRACT(EPOCH FROM (COALESCE(c.end_date, CURRENT_TIMESTAMP) - c.start_date)) / 86400 " +
           "FROM contracts c " +
           "WHERE c.agreed_amount BETWEEN :minAmount AND :maxAmount " +
           "AND (:status IS NULL OR c.status::text = :status) " +
           "ORDER BY c.agreed_amount DESC", nativeQuery = true)
    List<Object[]> searchContractsByBudgetRange(@Param("minAmount") Double minAmount,
                                                @Param("maxAmount") Double maxAmount,
                                                @Param("status") String status);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM contracts WHERE status IN ('COMPLETED', 'TERMINATED') AND created_at < :cutoffDate", nativeQuery = true)
    int purgeOldContracts(@Param("cutoffDate") LocalDateTime cutoffDate);

    // --- S4-F8: Freelancer Performance ---
    @Query(value = "SELECT " +
           "COUNT(c.id), " +
           "COALESCE(SUM(CASE WHEN c.status = 'COMPLETED' THEN 1 ELSE 0 END), 0), " +
           "COALESCE(SUM(c.agreed_amount), 0), " +
           "COALESCE(SUM(CASE WHEN c.status = 'COMPLETED' THEN c.agreed_amount ELSE 0 END), 0), " +
           "AVG(CASE WHEN c.status = 'COMPLETED' THEN EXTRACT(EPOCH FROM (c.end_date - c.start_date)) / 86400.0 ELSE NULL END) " +
           "FROM contracts c " +
           "WHERE c.freelancer_id = :freelancerId " +
           "AND c.created_at >= :startDate AND c.created_at <= :endDate", nativeQuery = true)
    List<Object[]> getFreelancerPerformanceAggregates(@Param("freelancerId") Long freelancerId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    // S4-F9: returns freelancer_id and job_id so ContractService can enrich via Feign
    @Query(value = "SELECT " +
           "c.id, " +
           "c.freelancer_id, " +
           "c.job_id, " +
           "c.agreed_amount, " +
           "CAST(c.metadata->>'progressPercentage' AS numeric), " +
           "CAST(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - COALESCE(CAST(c.metadata->>'lastActivityDate' AS TIMESTAMP), c.created_at))) / 86400 AS INTEGER) " +
           "FROM contracts c " +
           "WHERE c.status = 'ACTIVE' " +
           "AND CAST(c.metadata->>'progressPercentage' AS numeric) <= :maxProgress " +
           "AND CAST(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - COALESCE(CAST(c.metadata->>'lastActivityDate' AS TIMESTAMP), c.created_at))) / 86400 AS INTEGER) > :stalledDays", nativeQuery = true)
    List<Object[]> findStalledContracts(@Param("maxProgress") Double maxProgress, @Param("stalledDays") Integer stalledDays);

    // --- S4-F5: Metadata JSONB Filter ---
    @Query(value = "SELECT * FROM contracts WHERE jsonb_extract_path_text(metadata, :key) = :value", nativeQuery = true)
    List<Contract> findByMetadataEq(@Param("key") String key, @Param("value") String value);

    @Query(value = "SELECT * FROM contracts WHERE CAST(jsonb_extract_path_text(metadata, :key) AS numeric) > :value", nativeQuery = true)
    List<Contract> findByMetadataGt(@Param("key") String key, @Param("value") Double value);

    @Query(value = "SELECT * FROM contracts WHERE CAST(jsonb_extract_path_text(metadata, :key) AS numeric) < :value", nativeQuery = true)
    List<Contract> findByMetadataLt(@Param("key") String key, @Param("value") Double value);
}