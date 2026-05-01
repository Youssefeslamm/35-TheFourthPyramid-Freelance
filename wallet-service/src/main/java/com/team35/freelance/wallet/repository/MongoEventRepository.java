package com.team35.freelance.wallet.repository;

import com.team35.freelance.wallet.common.event.PayoutAuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoEventRepository extends MongoRepository<PayoutAuditEvent, String> {
}