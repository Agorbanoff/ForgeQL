package com.example.SigmaQL.controller;

import com.example.SigmaQL.common.exceptions.WrongCredentialsException;
import com.example.SigmaQL.controller.dtos.req.RefreshTokenDTO;
import com.example.SigmaQL.controller.dtos.res.TokenResponseDTO;
import com.example.SigmaQL.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/token")
public class TokenController {

    private final JwtService jwtService;

    public TokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDTO> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) throws WrongCredentialsException {

        String newAccessToken = jwtService.refreshAccessToken(refreshToken);
        TokenResponseDTO tokenResponseDTO = new TokenResponseDTO("Bearer", newAccessToken, null);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(tokenResponseDTO);
    }
}