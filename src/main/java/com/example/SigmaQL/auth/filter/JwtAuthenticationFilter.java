package com.example.SigmaQL.auth.filter;

import com.example.SigmaQL.auth.JwtValidation;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidation jwtValidation;

    @Autowired
    public JwtAuthenticationFilter(JwtValidation jwtValidation) {
        this.jwtValidation = jwtValidation;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractAccessTokenFromCookies(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtValidation.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtValidation.parseClaims(token);

        Integer userId = Integer.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, email);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        AuthorityUtils.NO_AUTHORITIES
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String extractAccessTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return user.userId();
    }
}