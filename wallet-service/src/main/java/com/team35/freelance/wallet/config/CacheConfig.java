package com.team35.freelance.wallet.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "wallet-service::payout",
                "wallet-service::promo-code",
                "wallet-service::payout-promo",
                "wallet-service::S5-F1",
                "wallet-service::S5-F3",
                "wallet-service::S5-F6",
                "wallet-service::S5-F8",
                "wallet-service::S5-F9",
                "wallet-service::S5-F10",
                "wallet-service::S5-F11"
        );
    }
}
