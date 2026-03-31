package com.example.service;

import com.example.auth.JwtValidation;
import com.example.auth.RefreshTokenHasher;
import com.example.common.exceptions.WrongCredentialsException;
import com.example.persistence.model.JwtEntity;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.JwtRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final long ACCESS_EXPIRATION_MS = 1000L * 60 * 15;
    private static final long REFRESH_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 30;

    private final JwtRepository jwtRepository;
    private final JwtValidation jwtValidation;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Key key;

    public JwtService(
            JwtRepository jwtRepository,
            JwtValidation jwtValidation,
            RefreshTokenHasher refreshTokenHasher,
            Key key
    ) {
        this.jwtRepository = jwtRepository;
        this.jwtValidation = jwtValidation;
        this.refreshTokenHasher = refreshTokenHasher;
        this.key = key;
    }

    public String generateAccessToken(Integer userId, String email) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ACCESS_EXPIRATION_MS))
                .setId(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Integer userId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + REFRESH_EXPIRATION_MS))
                .setId(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
    }

    @Transactional
    public void revokeAllUserTokens(UserAccountEntity user) {
        List<JwtEntity> activeTokens = jwtRepository.findAllByUserAccountAndRevokedFalse(user);
        for (JwtEntity token : activeTokens) {
            token.setRevoked(true);
        }
        jwtRepository.saveAll(activeTokens);
    }

    @Transactional
    public void saveRefreshToken(String rawRefreshToken, UserAccountEntity user) {
        Claims claims = jwtValidation.parseClaims(rawRefreshToken);

        JwtEntity entity = new JwtEntity();
        entity.setJti(claims.getId());
        entity.setTokenHash(refreshTokenHasher.hash(rawRefreshToken));
        entity.setIssuedAt(claims.getIssuedAt().toInstant());
        entity.setExpiresAt(claims.getExpiration().toInstant());
        entity.setRevoked(false);
        entity.setUserAccount(user);

        jwtRepository.save(entity);
    }

    @Transactional
    public String refreshAccessToken(String rawRefreshToken) throws WrongCredentialsException {
        if (!jwtValidation.validateToken(rawRefreshToken)) {
            throw new WrongCredentialsException("Invalid credentials");
        }

        Claims claims = jwtValidation.parseClaims(rawRefreshToken);
        String jti = claims.getId();
        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);

        JwtEntity storedToken = jwtRepository.findByJti(jti)
                .orElseThrow(() -> new WrongCredentialsException("Invalid credentials"));

        if (storedToken.isRevoked()) {
            throw new WrongCredentialsException("Invalid credentials");
        }

        if (!storedToken.getTokenHash().equals(tokenHash)) {
            throw new WrongCredentialsException("Invalid credentials");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new WrongCredentialsException("Invalid credentials");
        }

        UserAccountEntity user = storedToken.getUserAccount();
        return generateAccessToken(user.getId(), user.getEmail());
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (!jwtValidation.validateToken(rawRefreshToken)) {
            return;
        }

        Claims claims = jwtValidation.parseClaims(rawRefreshToken);

        jwtRepository.findByJti(claims.getId()).ifPresent(token -> {
            token.setRevoked(true);
            jwtRepository.save(token);
        });
    }

    public Integer extractUserId(String token) {
        Claims claims = jwtValidation.parseClaims(token);
        return Integer.valueOf(claims.getSubject());
    }

    @Transactional
    public void deleteExpiredTokens() {
        jwtRepository.deleteAllByExpiresAtBefore(Instant.now());
    }
}