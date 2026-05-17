package com.team35.freelance.user.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignInternalAuthConfig {

    @Bean
    public RequestInterceptor internalCallInterceptor() {
        return template -> template.header("X-INTERNAL-CALL", "true");
    }
}
