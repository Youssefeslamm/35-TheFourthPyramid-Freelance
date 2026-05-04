package com.team35.freelance.wallet.repository;

import com.team35.freelance.wallet.common.event.PayoutAuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MongoEventRepository extends MongoRepository<PayoutAuditEvent, String> {

    List<PayoutAuditEvent> findByActionInAndTimestampBetween(
            List<String> actions,
            LocalDateTime start,
            LocalDateTime end
    );
}