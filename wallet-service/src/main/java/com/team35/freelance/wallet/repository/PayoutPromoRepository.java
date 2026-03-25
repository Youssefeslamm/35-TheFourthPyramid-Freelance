package com.team35.freelance.wallet.repository;

import com.team35.freelance.wallet.model.PayoutPromo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayoutPromoRepository extends JpaRepository<PayoutPromo, Long> {

    List<PayoutPromo> findByPayoutId(Long payoutId);

    List<PayoutPromo> findByPromoCodeId(Long promoCodeId);

    Optional<PayoutPromo> findByPayoutIdAndPromoCodeId(Long payoutId, Long promoCodeId);
}
