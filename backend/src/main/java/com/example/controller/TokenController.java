package com.example.controller;

import com.example.common.exceptions.WrongCredentialsException;
import com.example.service.AuthCookieService;
import com.example.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/token")
public class TokenController {

    private final JwtService jwtService;
    private final AuthCookieService authCookieService;

    public TokenController(JwtService jwtService, AuthCookieService authCookieService) {
        this.jwtService = jwtService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                        HttpServletResponse response) throws WrongCredentialsException {

        JwtService.AuthTokens tokens = jwtService.rotateRefreshToken(refreshToken);
        authCookieService.addRefreshTokenCookie(response, tokens.refreshToken());
        authCookieService.addAccessTokenCookie(response, tokens.accessToken());

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
