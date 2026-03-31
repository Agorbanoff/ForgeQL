package com.example.service;

import com.example.common.exceptions.EmailAlreadyInUseException;
import com.example.common.exceptions.WrongCredentialsException;
import com.example.controller.dtos.request.UserLogInDTO;
import com.example.controller.dtos.request.UserSignUpDTO;
import com.example.controller.dtos.response.TokenResponseDTO;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            JwtService jwtService,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenResponseDTO signUp(UserSignUpDTO dto) throws EmailAlreadyInUseException {
        if (userAccountRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyInUseException("Email is already in use");
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        UserAccountEntity savedUser = userAccountRepository.save(user);

        String accessToken = jwtService.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId());
        jwtService.saveRefreshToken(refreshToken, savedUser);

        return new TokenResponseDTO("Bearer", accessToken, refreshToken);
    }

    @Transactional
    public TokenResponseDTO logIn(UserLogInDTO dto) throws WrongCredentialsException {
        UserAccountEntity user = userAccountRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new WrongCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new WrongCredentialsException("Invalid credentials");
        }

        jwtService.revokeAllUserTokens(user);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        jwtService.saveRefreshToken(refreshToken, user);

        return new TokenResponseDTO("Bearer", accessToken, refreshToken);
    }

    @Transactional
    public void logOut(String refreshToken) {
        jwtService.revokeRefreshToken(refreshToken);
    }
}