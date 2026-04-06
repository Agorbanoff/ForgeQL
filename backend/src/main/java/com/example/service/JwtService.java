package com.example.service;

import com.example.auth.JwtValidation;
import com.example.auth.RefreshTokenHasher;
import com.example.common.exceptions.WrongCredentialsException;
import com.example.persistence.model.JwtEntity;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.JwtRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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

    public record AuthTokens(String accessToken, String refreshToken) {
    }

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
                .setIssuer(JwtValidation.ISSUER)
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim(JwtValidation.TOKEN_TYPE_CLAIM, JwtValidation.ACCESS_TOKEN_TYPE)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ACCESS_EXPIRATION_MS))
                .setId(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Integer userId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setIssuer(JwtValidation.ISSUER)
                .setSubject(String.valueOf(userId))
                .claim(JwtValidation.TOKEN_TYPE_CLAIM, JwtValidation.REFRESH_TOKEN_TYPE)
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
        Claims claims = jwtValidation.parseRefreshClaims(rawRefreshToken);

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
    public AuthTokens rotateRefreshToken(String rawRefreshToken) throws WrongCredentialsException {
        if (!jwtValidation.validateRefreshToken(rawRefreshToken)) {
            throw new WrongCredentialsException("Invalid credentials");
        }

        Claims claims = jwtValidation.parseRefreshClaims(rawRefreshToken);
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

        if (!storedToken.getUserAccount().getId().equals(Integer.valueOf(claims.getSubject()))) {
            throw new WrongCredentialsException("Invalid credentials");
        }

        storedToken.setRevoked(true);
        jwtRepository.save(storedToken);

        UserAccountEntity user = storedToken.getUserAccount();
        String accessToken = generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = generateRefreshToken(user.getId());
        saveRefreshToken(refreshToken, user);

        return new AuthTokens(accessToken, refreshToken);
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        if (!jwtValidation.validateRefreshToken(rawRefreshToken)) {
            return;
        }

        Claims claims = jwtValidation.parseRefreshClaims(rawRefreshToken);

        jwtRepository.findByJti(claims.getId()).ifPresent(token -> {
            token.setRevoked(true);
            jwtRepository.save(token);
        });
    }

    public Integer extractUserId(String token) {
        Claims claims = jwtValidation.parseAccessClaims(token);
        return Integer.valueOf(claims.getSubject());
    }

    @Transactional
    public void deleteExpiredTokens() {
        jwtRepository.deleteAllByExpiresAtBefore(Instant.now());
    }
}
