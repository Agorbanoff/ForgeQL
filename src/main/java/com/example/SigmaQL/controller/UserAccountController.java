package com.example.SigmaQL.controller;

import com.example.SigmaQL.auth.filter.AuthenticatedUser;
import com.example.SigmaQL.common.exceptions.EmailAlreadyInUseException;
import com.example.SigmaQL.common.exceptions.WrongCredentialsException;
import com.example.SigmaQL.controller.dtos.req.CurrentUserDTO;
import com.example.SigmaQL.controller.dtos.req.UserLogInDTO;
import com.example.SigmaQL.controller.dtos.req.UserSignUpDTO;
import com.example.SigmaQL.controller.dtos.res.TokenResponseDTO;
import com.example.SigmaQL.service.AuthCookieService;
import com.example.SigmaQL.service.UserAccountService;
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
    public ResponseEntity<TokenResponseDTO> signUp(@Valid @RequestBody UserSignUpDTO userSignUpDTO, HttpServletResponse httpServletResponse) throws EmailAlreadyInUseException {
        TokenResponseDTO tokenResponseDTO = userAccountService.signUp(userSignUpDTO);

        authCookieService.addRefreshTokenCookie(httpServletResponse, tokenResponseDTO.getRefreshToken());
        tokenResponseDTO.setRefreshToken(null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tokenResponseDTO);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<TokenResponseDTO> logIn(@Valid @RequestBody UserLogInDTO userLogInDTO, HttpServletResponse httpServletResponse) throws WrongCredentialsException {
        TokenResponseDTO tokenResponseDTO = userAccountService.logIn(userLogInDTO);

        authCookieService.addRefreshTokenCookie(httpServletResponse, tokenResponseDTO.getRefreshToken());
        tokenResponseDTO.setRefreshToken(null);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(tokenResponseDTO);
    }

    @PostMapping(value = "/logout")
    public ResponseEntity<Void> logOut(@CookieValue(name = "refresh_token", required = false) String refreshToken, HttpServletResponse httpServletResponse) throws WrongCredentialsException {
        userAccountService.logOut(refreshToken);
        authCookieService.clearRefreshTokenCookie(httpServletResponse);

        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
