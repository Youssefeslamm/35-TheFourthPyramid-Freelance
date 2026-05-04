package com.team35.freelance.wallet.common.refund;

import com.team35.freelance.wallet.model.Payout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MilestoneReversalStrategy implements RefundStrategy {

    private final JdbcTemplate jdbcTemplate;

    public MilestoneReversalStrategy(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RefundResult calculateRefund(Payout payout, RefundRequest request) {

        Long contractId = payout.getContractId();

        if (contractId == null) {
            throw new RuntimeException("Payout not linked to contract");
        }

        Double refundAmount = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(m.amount), 0)
                FROM proposal_milestones m
                WHERE m.proposal_id = (
                    SELECT proposal_id FROM contracts WHERE id = ?
                )
                AND m.status NOT IN ('COMPLETED', 'APPROVED')
                """,
                Double.class,
                contractId
        );
        System.out.println(">>> MILESTONE STRATEGY EXECUTED");
        System.out.println(">>> CONTRACT ID = " + contractId);
        System.out.println(">>> REFUND FROM DB = " + refundAmount);
        return new RefundResult(refundAmount, "MILESTONE_ONLY");
    }
}