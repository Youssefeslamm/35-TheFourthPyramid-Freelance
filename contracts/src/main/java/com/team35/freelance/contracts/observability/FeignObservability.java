package com.team35.freelance.contracts.observability;

import feign.InvocationContext;
import feign.RequestInterceptor;
import feign.ResponseInterceptor;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class FeignObservability {

    private static final Logger log = LoggerFactory.getLogger(FeignObservability.class);

    public static final String MDC_FEIGN_CLIENT = "feignClient";
    public static final String MDC_FEIGN_METHOD = "feignMethod";

    private FeignObservability() {
    }

    public static RequestInterceptor requestInterceptor(String correlationHeader, String correlationMdcKey) {
        return template -> {
            String correlationId = MDC.get(correlationMdcKey);
            if (correlationId != null) {
                template.header(correlationHeader, correlationId);
            }
            String target = template.feignTarget() != null ? template.feignTarget().name() : "feign";
            String method = template.method();
            MDC.put(MDC_FEIGN_CLIENT, target);
            MDC.put(MDC_FEIGN_METHOD, method);
            log.info("Calling {}.{} with args={}", target, method,
                    template.body() != null ? new String(template.body()) : "none");
        };
    }

    public static ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.warn("Feign call to {} failed: {}", methodKey, response.status());
            return new ErrorDecoder.Default().decode(methodKey, response);
        };
    }

    public static ResponseInterceptor successResponseInterceptor() {
        return (InvocationContext invocationContext, ResponseInterceptor.Chain chain) -> {
            try {
                Object result = chain.next(invocationContext);
                String client = MDC.get(MDC_FEIGN_CLIENT);
                String method = MDC.get(MDC_FEIGN_METHOD);
                if (client != null && method != null) {
                    log.info("{}.{} returned successfully", client, method);
                }
                return result;
            } finally {
                MDC.remove(MDC_FEIGN_CLIENT);
                MDC.remove(MDC_FEIGN_METHOD);
            }
        };
    }
}
