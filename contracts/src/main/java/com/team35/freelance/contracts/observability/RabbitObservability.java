package com.team35.freelance.contracts.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;

import java.util.Map;

public final class RabbitObservability {

    private static final Logger log = LoggerFactory.getLogger(RabbitObservability.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String ROUTING_KEY_MDC_KEY = "routingKey";

    private RabbitObservability() {
    }

    public static void applyInboundMdc(Message message) {
        if (message == null || message.getMessageProperties() == null) {
            return;
        }
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        if (routingKey != null && !routingKey.isBlank()) {
            MDC.put(ROUTING_KEY_MDC_KEY, routingKey);
        }
        String correlationId = resolveCorrelationId(message.getMessageProperties().getHeaders());
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        }
    }

    public static void applyInboundMdc(String routingKey, Map<String, Object> headers) {
        if (routingKey != null && !routingKey.isBlank()) {
            MDC.put(ROUTING_KEY_MDC_KEY, routingKey);
        }
        String correlationId = resolveCorrelationId(headers);
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        }
    }

    public static void publish(String routingKey, String entityKey, Object entityValue, Runnable publishAction) {
        try {
            MDC.put(ROUTING_KEY_MDC_KEY, routingKey);
            if (entityKey != null && entityValue != null) {
                MDC.put(entityKey, String.valueOf(entityValue));
            }
            publishAction.run();
            log.info("Published {} for {}={}", routingKey, entityKey, entityValue);
        } finally {
            MDC.remove(ROUTING_KEY_MDC_KEY);
            if (entityKey != null) {
                MDC.remove(entityKey);
            }
        }
    }

    public static void logConsuming(String routingKey, String entityKey, Object entityValue) {
        log.info("Consuming {} for {}={}", routingKey, entityKey, entityValue);
    }

    public static void logProcessed(String routingKey, String entityKey, Object entityValue) {
        log.info("Processed {} for {}={}", routingKey, entityKey, entityValue);
    }

    public static void logFailed(String routingKey, String message) {
        log.error("Failed to process {}: {}", routingKey, message);
    }

    public static void logFailed(String routingKey, String message, Throwable cause) {
        log.error("Failed to process {}: {}", routingKey, message, cause);
    }

    public static void clearConsumerMdc(String... extraKeys) {
        MDC.remove(ROUTING_KEY_MDC_KEY);
        MDC.remove(CORRELATION_ID_MDC_KEY);
        if (extraKeys != null) {
            for (String key : extraKeys) {
                MDC.remove(key);
            }
        }
    }

    private static String resolveCorrelationId(Map<String, Object> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        Object correlationId = headers.get(CORRELATION_ID_MDC_KEY);
        if (correlationId == null) {
            correlationId = headers.get(CORRELATION_ID_HEADER);
        }
        return correlationId != null ? correlationId.toString() : null;
    }
}
