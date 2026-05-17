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
        log.info("Calling {}.{} with args={}", clientName, operation, "none");
        try {
            T result = call.get();
            log.info("{}.{} returned successfully", clientName, operation);
            return result;
        } catch (FeignException.NotFound e) {
            log.warn("Feign call to {}.{} failed: {}", clientName, operation, e.status());
            return notFoundFallback;
        } catch (FeignException e) {
            log.warn("Feign call to {}.{} failed: {}", clientName, operation, e.status());
            throw e;
        } catch (RuntimeException e) {
            log.warn("Feign call to {}.{} failed: {}", clientName, operation, e.getMessage());
            throw e;
        }
    }
}
