package com.team35.freelance.contract.config;

import feign.RequestInterceptor;
import jakarta.servlet.DispatcherType;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.EnumSet;

// @Configuration moktamel: el correlation (filter + servlet registration + Feign interceptor) kolha fel class da
@Configuration
public class FeignCorrelationConfig {

    // el filter instance — el registration fe order (abl el security)
    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    // ye7ot el filter fel servlet chain b HIGHEST_PRECEDENCE 3ashan yesh8al abl el security w el JWT
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter correlationIdFilter) {
        FilterRegistrationBean<CorrelationIdFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(correlationIdFilter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        reg.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        return reg;
    }

    // Feign tal3: yeb3at nafs el correlationId men el MDC fel header X-Correlation-ID
    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-ID", correlationId);
            }
        };
    }
}
