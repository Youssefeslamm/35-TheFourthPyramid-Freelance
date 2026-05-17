package com.team35.freelance.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final SecretKey signingKey;

    public JwtGatewayFilter(@Value("${jwt.secret:${JWT_SECRET:0123456789abcdef0123456789abcdef01234567}}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = resolveCorrelationId(exchange);

        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        if (isPublicPath(path)) {
            return chain.filter(withCorrelationId(exchange, correlationId));
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        Claims claims = parseClaims(authorization.substring(BEARER_PREFIX.length()));
        if (claims == null || isExpired(claims)) {
            return unauthorized(exchange);
        }

        String userId = resolveUserId(claims);
        String userRole = claims.get("role", String.class);
        if (userId == null || userRole == null || userRole.isBlank()) {
            return unauthorized(exchange);
        }

        ServerWebExchange enrichedExchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> {
                    headers.set(USER_ID_HEADER, userId);
                    headers.set(USER_ROLE_HEADER, userRole);
                    headers.set(CORRELATION_ID_HEADER, correlationId);
                }))
                .build();

        return chain.filter(enrichedExchange);
    }

    private boolean isPublicPath(String path) {
        return path.equals("/api/auth")
                || path.startsWith("/api/auth/")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.equals("/actuator/prometheus")
                || path.equals("/actuator/info")
                || path.contains("/health");
    }

    private ServerWebExchange withCorrelationId(ServerWebExchange exchange, String correlationId) {
        return exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(CORRELATION_ID_HEADER, correlationId)))
                .build();
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    private String resolveUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId == null) {
            userId = claims.get("uid");
        }
        return userId == null ? null : String.valueOf(userId);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
