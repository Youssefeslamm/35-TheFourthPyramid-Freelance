package com.team35.freelance.contract.security;
import com.team35.freelance.contract.common.config.JwtConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(JwtConfigurationManager.getInstance().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration() == null || claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
