package com.team35.freelance.user.common.config;

public class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private String secret;
    private long expiration;

    // 🔒 PRIVATE CONSTRUCTOR (MANDATORY)
    private JwtConfigurationManager() {
        // Read from environment variables (as required)
        this.secret = System.getenv().getOrDefault(
                "JWT_SECRET",
                "0123456789abcdef0123456789abcdef01234567"
        );

        this.expiration = Long.parseLong(
                System.getenv().getOrDefault("JWT_EXPIRATION_MS", "86400000")
        );
    }

    // ✅ THREAD-SAFE SINGLETON
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

    // ✅ GETTERS
    public String getSecret() {
        return secret;
    }

    public long getExpiration() {
        return expiration;
    }
}