package com.team35.freelance.job.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("job-service::job", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("job-service::S2-F1", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("job-service::S2-F3", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("job-service::S2-F5", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("job-service::S2-F6", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("job-service::S2-F9", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put("job-service::S2-F10", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("job-service::S2-F12", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
