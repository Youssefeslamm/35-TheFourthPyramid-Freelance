package com.team35.freelance.wallet.repository;

import com.team35.freelance.wallet.event.PayoutAuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PayoutAuditEventRepository extends MongoRepository<PayoutAuditEvent, String> {
}