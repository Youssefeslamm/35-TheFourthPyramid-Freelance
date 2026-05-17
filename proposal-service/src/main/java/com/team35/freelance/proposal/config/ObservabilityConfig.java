package com.team35.freelance.proposal.config;

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
                        String correlationId = org.slf4j.MDC.get("correlationId");
                        if (correlationId != null) {
                            message.getMessageProperties().setHeader("X-Correlation-ID", correlationId);
                        }
                        return message;
                    });
                }
                return bean;
            }
        };
    }
}
