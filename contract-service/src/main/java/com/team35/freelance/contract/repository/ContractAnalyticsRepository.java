package com.team35.freelance.contract.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ContractAnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public long countContractsInRange(LocalDateTime start, LocalDateTime end) {
        return (long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM contracts WHERE start_date BETWEEN :start AND :end")
            .setParameter("start", start)
            .setParameter("end", end)
            .getSingleResult();
    }

    public double avgContractValue(LocalDateTime start, LocalDateTime end) {
        Object result = entityManager.createNativeQuery(
            "SELECT AVG(agreed_amount) FROM contracts WHERE start_date BETWEEN :start AND :end")
            .setParameter("start", start)
            .setParameter("end", end)
            .getSingleResult();
        return result == null ? 0.0 : ((Number) result).doubleValue();
    }

    public long countCompletedInRange(LocalDateTime start, LocalDateTime end) {
        return (long) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM contracts WHERE start_date BETWEEN :start AND :end AND status = 'COMPLETED'")
            .setParameter("start", start)
            .setParameter("end", end)
            .getSingleResult();
    }

    public double avgDurationDays(LocalDateTime start, LocalDateTime end) {
        Object result = entityManager.createNativeQuery(
            "SELECT AVG(EXTRACT(EPOCH FROM (end_date - start_date))/86400) FROM contracts " +
            "WHERE start_date BETWEEN :start AND :end AND status = 'COMPLETED' AND end_date IS NOT NULL")
            .setParameter("start", start)
            .setParameter("end", end)
            .getSingleResult();
        return result == null ? 0.0 : ((Number) result).doubleValue();
    }

    public Map<String, Long> countByStatus(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = entityManager.createNativeQuery(
            "SELECT status, COUNT(*) FROM contracts WHERE start_date BETWEEN :start AND :end GROUP BY status")
            .setParameter("start", start)
            .setParameter("end", end)
            .getResultList();
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }
}
