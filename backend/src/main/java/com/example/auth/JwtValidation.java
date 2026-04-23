package com.example.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Objects;

@Service
public class JwtValidation {

    public static final String ISSUER = "forgeql";
    public static final String TOKEN_TYPE_CLAIM = "token_type";
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final Key key;

    public JwtValidation(Key key) {
        this.key = key;
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAccessClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims parseAccessClaims(String token) {
        return parseClaims(token, ACCESS_TOKEN_TYPE);
    }

    public Claims parseRefreshClaims(String token) {
        return parseClaims(token, REFRESH_TOKEN_TYPE);
    }

    private Claims parseClaims(String token, String expectedTokenType) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (!Objects.equals(ISSUER, claims.getIssuer())) {
            throw new IllegalArgumentException("Invalid token issuer");
        }

        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!Objects.equals(expectedTokenType, tokenType)) {
            throw new IllegalArgumentException("Invalid token type");
        }

        return claims;
    }
}