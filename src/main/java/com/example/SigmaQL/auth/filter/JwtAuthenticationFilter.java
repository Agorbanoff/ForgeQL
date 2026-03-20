package com.example.SigmaQL.auth.filter;

import com.example.SigmaQL.auth.JwtValidation;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, httpServletResponse);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtValidation.validateToken(token)) {
            filterChain.doFilter(request, httpServletResponse);
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

        filterChain.doFilter(request, httpServletResponse);
    }
}