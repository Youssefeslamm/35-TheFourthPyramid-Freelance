package com.team35.freelance.contract.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtValidator {

    private final ObjectMapper objectMapper;
    private final byte[] jwtSecretBytes;

    public JwtValidator(ObjectMapper objectMapper, @Value("${jwt.secret}") String jwtSecret) {
        this.objectMapper = objectMapper;
        this.jwtSecretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    public void validateAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7).trim();
        validateToken(token);
    }

    private void validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw unauthorized();
            }

            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSignature = hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8), jwtSecretBytes);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw unauthorized();
            }

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (payload.has("exp")) {
                long exp = payload.get("exp").asLong();
                if (Instant.now().getEpochSecond() >= exp) {
                    throw unauthorized();
                }
            }
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    private byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT token");
    }
}
