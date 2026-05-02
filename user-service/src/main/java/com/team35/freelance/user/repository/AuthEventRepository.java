package com.team35.freelance.user.repository;

import com.team35.freelance.user.event.AuthEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuthEventRepository extends MongoRepository<AuthEvent, String> {
}