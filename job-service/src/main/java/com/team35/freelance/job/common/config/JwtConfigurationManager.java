package com.team35.freelance.job.common.config;

public class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private final long expirationMs;

    private JwtConfigurationManager() {
        this.secret = System.getenv().getOrDefault("JWT_SECRET", "default-secret");
        this.expirationMs = Long.parseLong(
                System.getenv().getOrDefault("JWT_EXPIRATION_MS", "86400000")
        );
    }

    public static JwtConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (JwtConfigurationManager.class) {
                if (instance == null) {
                    instance = new JwtConfigurationManager();
                }
            }
        }
        return instance;
    }

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}

