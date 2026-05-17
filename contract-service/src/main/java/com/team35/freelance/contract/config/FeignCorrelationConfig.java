package com.team35.freelance.contract.config;

import com.team35.freelance.contracts.observability.FeignObservability;
import feign.RequestInterceptor;
import feign.ResponseInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.DispatcherType;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.BeanPostProcessor;
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

    @Bean
    public FilterRegistrationBean<HttpRequestLoggingFilter> httpRequestLoggingFilterRegistration() {
        FilterRegistrationBean<HttpRequestLoggingFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new HttpRequestLoggingFilter());
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        reg.addUrlPatterns("/*");
        reg.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        return reg;
    }

    @Bean
    public BeanPostProcessor rabbitTemplateCorrelationProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RabbitTemplate template) {
                    template.setBeforePublishPostProcessors(message -> {
                        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
                        if (correlationId != null) {
                            message.getMessageProperties().setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
                        }
                        return message;
                    });
                }
                return bean;
            }
        };
    }

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return FeignObservability.requestInterceptor(
                CorrelationIdFilter.CORRELATION_ID_HEADER,
                CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
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
