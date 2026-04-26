package com.example.controller;

import com.example.auth.filter.AuthenticatedUser;
import com.example.common.exceptions.EmailAlreadyInUseException;
import com.example.common.exceptions.UserNotFoundException;
import com.example.common.exceptions.WrongCredentialsException;
import com.example.controller.dtos.request.UserLogInDTO;
import com.example.controller.dtos.request.UserSignUpDTO;
import com.example.controller.dtos.response.CurrentUserDTO;
import com.example.controller.dtos.response.TokenResponseDTO;
import com.example.persistence.model.UserAccountEntity;
import com.example.service.AuthCookieService;
import com.example.service.UserAccountService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/account")
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
    public ResponseEntity<CurrentUserDTO> me(Authentication authentication) throws UserNotFoundException {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        UserAccountEntity userAccount = userAccountService.getUserById(user.userId());
        return ResponseEntity.ok(new CurrentUserDTO(
                user.userId(),
                user.email(),
                userAccount.getUsername(),
                userAccount.getGlobalRole()
        ));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of("token", csrfToken.getToken()));
    }

    @PostMapping(value = "/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody UserSignUpDTO userSignUpDTO) throws EmailAlreadyInUseException {
        userAccountService.signUp(userSignUpDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    @PostMapping(value = "/login")
    public ResponseEntity<Void> logIn(@Valid @RequestBody UserLogInDTO userLogInDTO, HttpServletResponse httpServletResponse) throws WrongCredentialsException {
        TokenResponseDTO tokenResponseDTO = userAccountService.logIn(userLogInDTO);

        authCookieService.addRefreshTokenCookie(httpServletResponse, tokenResponseDTO.refreshToken());
        authCookieService.addAccessTokenCookie(httpServletResponse, tokenResponseDTO.accessToken());

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

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentAccount(
            Authentication authentication,
            HttpServletResponse httpServletResponse
    ) throws UserNotFoundException {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        userAccountService.deleteAccount(user.userId());
        authCookieService.clearCookies(httpServletResponse);

        return ResponseEntity.noContent().build();
    }
}
