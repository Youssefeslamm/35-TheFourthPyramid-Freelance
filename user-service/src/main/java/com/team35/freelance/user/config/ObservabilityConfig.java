package com.team35.freelance.user.config;

import jakarta.servlet.DispatcherType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.EnumSet;

@Configuration
public class ObservabilityConfig {

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(filter);
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
                        String correlationId = org.slf4j.MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
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
}
