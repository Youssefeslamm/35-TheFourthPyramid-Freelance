package com.team35.freelance.user.repository;

import com.team35.freelance.user.common.event.AuthEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthEventRepository extends MongoRepository<AuthEvent, String> {

    Page<AuthEvent> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
}

