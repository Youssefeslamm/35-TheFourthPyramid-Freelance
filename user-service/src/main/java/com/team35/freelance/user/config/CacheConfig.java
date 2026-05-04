package com.team35.freelance.user.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // §8.1 TTL rules:
        // Activity feeds / search results = 5 min
        // Dashboards / analytics / reports = 10 min
        // Entity detail views = 15 min
        RedisCacheConfiguration defaultCfg = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCfg)
                .withInitialCacheConfigurations(Map.of(
                        // Entity detail — 15 min
                        "user-service::user",       RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(15)),
                        // F1 search results — 5 min
                        "user-service::S1-F1",      RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)),
                        // F3 DTO — 10 min
                        "user-service::S1-F3",      RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)),
                        // F5 JSONB query — 5 min
                        "user-service::S1-F5",      RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)),
                        // F6 report — 10 min
                        "user-service::S1-F6",      RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)),
                        // F9 combined — 10 min
                        "user-service::S1-F9",      RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)),
                        // S1-F12 activity feed — 5 min (§8.1 activity feeds)
                        "user-service::S1-F12",     RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5))
                ))
                .build();
    }

    @Bean("customKeyGenerator")
    public KeyGenerator customKeyGenerator() {
        return (Object target, Method method, Object... params) ->
                method.getName() + "::" + Arrays.deepToString(params);
    }
}