package com.example.auth;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Configuration
public class JwtKeyConfig {

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    public Key jwtSigningKey(@Value("${jwt.secret}") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 bytes. Set JWT_SECRET or jwt.secret to a value long enough for HMAC-SHA."
            );
        }

        return Keys.hmacShaKeyFor(secretBytes);
    }
}
