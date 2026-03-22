package com.team35.freelance.wallet.repository;

import com.team35.freelance.wallet.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCode(String code);
    boolean existsByCode(String code);
}