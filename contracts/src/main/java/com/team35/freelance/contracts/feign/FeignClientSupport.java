package com.team35.freelance.contracts.feign;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class FeignClientSupport {

    private static final Logger log = LoggerFactory.getLogger(FeignClientSupport.class);

    private FeignClientSupport() {
    }

    public static <T> T execute(String clientName, String operation, Supplier<T> call, T notFoundFallback) {
        try {
            return call.get();
        } catch (FeignException.NotFound e) {
            log.warn("{} {} returned 404: {}", clientName, operation, e.getMessage());
            return notFoundFallback;
        } catch (FeignException e) {
            log.error("{} {} failed with status {}: {}", clientName, operation, e.status(), e.getMessage());
            throw e;
        }
    }
}
