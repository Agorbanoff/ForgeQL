package com.example.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.common.exceptions.EmailAlreadyInUseException;
import com.example.common.exceptions.WrongCredentialsException;
import com.example.controller.dtos.request.CurrentUserDTO;
import com.example.controller.dtos.request.UserLogInDTO;
import com.example.controller.dtos.request.UserSignUpDTO;
import com.example.controller.dtos.response.TokenResponseDTO;
import com.example.service.AuthCookieService;
import com.example.service.UserAccountService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@CrossOrigin(origins = "http://localhost:5173")
public class UserAccountController {
    private final UserAccountService userAccountService;
    private final AuthCookieService authCookieService;

    @Autowired
    public UserAccountController(UserAccountService userAccountService,
                                 AuthCookieService authCookieService) {
        this.userAccountService = userAccountService;
        this.authCookieService = authCookieService;
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserDTO> me(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ResponseEntity.ok(new CurrentUserDTO(user.userId(), user.email()));
    }

    @PostMapping(value = "/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody UserSignUpDTO userSignUpDTO, HttpServletResponse httpServletResponse) throws EmailAlreadyInUseException {
        TokenResponseDTO tokenResponseDTO = userAccountService.signUp(userSignUpDTO);

        authCookieService.addRefreshTokenCookie(httpServletResponse, tokenResponseDTO.getRefreshToken());
        authCookieService.addAccessTokenCookie(httpServletResponse, tokenResponseDTO.getAccessToken());

        tokenResponseDTO.setAccessToken(null);
        tokenResponseDTO.setRefreshToken(null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping(value = "/login")
    public ResponseEntity<Void> logIn(@Valid @RequestBody UserLogInDTO userLogInDTO, HttpServletResponse httpServletResponse) throws WrongCredentialsException {
        TokenResponseDTO tokenResponseDTO = userAccountService.logIn(userLogInDTO);

        authCookieService.addRefreshTokenCookie(httpServletResponse, tokenResponseDTO.getRefreshToken());
        authCookieService.addAccessTokenCookie(httpServletResponse, tokenResponseDTO.getAccessToken());

        tokenResponseDTO.setAccessToken(null);
        tokenResponseDTO.setRefreshToken(null);

        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @PostMapping(value = "/logout")
    public ResponseEntity<Void> logOut(@CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse httpServletResponse) throws WrongCredentialsException {
        userAccountService.logOut(refreshToken);
        authCookieService.clearCookies(httpServletResponse);

        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
