package com.team35.freelance.wallet.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("wallet-service::payout",     defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("wallet-service::promo-code", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("wallet-service::payout-promo", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("wallet-service::S5-F1",  defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("wallet-service::S5-F3",  defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("wallet-service::S5-F6",  defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("wallet-service::S5-F8",  defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("wallet-service::S5-F9",  defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("wallet-service::S5-F10", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("wallet-service::S5-F11", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}