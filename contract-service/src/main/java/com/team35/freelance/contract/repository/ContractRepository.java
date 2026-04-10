package com.team35.freelance.contract.repository;

import com.team35.freelance.contract.model.Contract;
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

    @Query(value = "SELECT * FROM contracts WHERE freelancer_id = :userId AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Contract> findMostRecentActiveContractByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM users WHERE id = :userId", nativeQuery = true)
    int checkUserExists(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM contracts WHERE created_at BETWEEN :startDate AND :endDate " +
           "AND (:status IS NULL OR status = :status) ORDER BY created_at ASC", nativeQuery = true)
    List<Contract> findContractsInDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            @Param("status") String status);

    @Query(value = "SELECT c.id, u.name, j.title, c.agreed_amount, c.status, " +
           "EXTRACT(EPOCH FROM (COALESCE(c.end_date, CURRENT_TIMESTAMP) - c.start_date)) / 86400 " +
           "FROM contracts c " +
           "INNER JOIN users u ON u.id = c.freelancer_id " +
           "INNER JOIN jobs j ON j.id = c.job_id " +
           "WHERE c.agreed_amount BETWEEN :minAmount AND :maxAmount " +
           "AND (:status IS NULL OR c.status = :status) " +
           "ORDER BY c.agreed_amount DESC", nativeQuery = true)
    List<Object[]> searchContractsByBudgetRange(@Param("minAmount") Double minAmount,
                                                @Param("maxAmount") Double maxAmount,
                                                @Param("status") String status);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM contracts WHERE status IN ('COMPLETED', 'TERMINATED') AND created_at < :cutoffDate", nativeQuery = true)
    int purgeOldContracts(@Param("cutoffDate") LocalDateTime cutoffDate);
}
