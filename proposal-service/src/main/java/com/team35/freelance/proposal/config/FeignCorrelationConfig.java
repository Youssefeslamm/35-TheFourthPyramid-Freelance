package com.team35.freelance.proposal.config;

import com.team35.freelance.contracts.observability.FeignObservability;
import feign.RequestInterceptor;
import feign.ResponseInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignCorrelationConfig {

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return FeignObservability.requestInterceptor(
                CorrelationIdFilter.CORRELATION_ID_HEADER,
                CorrelationIdFilter.MDC_KEY);
    }

    @Bean
    public ErrorDecoder observabilityFeignErrorDecoder() {
        return FeignObservability.errorDecoder();
    }

    @Bean
    public ResponseInterceptor observabilityFeignResponseInterceptor() {
        return FeignObservability.successResponseInterceptor();
    }
}
