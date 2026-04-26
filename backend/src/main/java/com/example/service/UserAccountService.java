package com.example.service;

import com.example.common.exceptions.EmailAlreadyInUseException;
import com.example.common.exceptions.UserNotFoundException;
import com.example.common.exceptions.WrongCredentialsException;
import com.example.controller.dtos.request.UserLogInDTO;
import com.example.controller.dtos.request.UserSignUpDTO;
import com.example.controller.dtos.response.TokenResponseDTO;
import com.example.core.postgres.schema.registry.SchemaRegistryService;
import com.example.persistence.model.DataSourceEntity;
import com.example.persistence.model.UserAccountEntity;
import com.example.persistence.repository.DataSourceAccessRepository;
import com.example.persistence.repository.DataSourceRepository;
import com.example.persistence.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceAccessRepository dataSourceAccessRepository;
    private final JwtService jwtService;
    private final SchemaRegistryService schemaRegistryService;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            DataSourceRepository dataSourceRepository,
            DataSourceAccessRepository dataSourceAccessRepository,
            JwtService jwtService,
            SchemaRegistryService schemaRegistryService,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourceAccessRepository = dataSourceAccessRepository;
        this.jwtService = jwtService;
        this.schemaRegistryService = schemaRegistryService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void signUp(UserSignUpDTO dto) throws EmailAlreadyInUseException {
        if (userAccountRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyInUseException("Email is already in use");
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));

        userAccountRepository.save(user);
    }

    @Transactional
    public TokenResponseDTO logIn(UserLogInDTO dto) throws WrongCredentialsException {
        UserAccountEntity user = userAccountRepository.findByEmail(dto.email())
                .orElseThrow(() -> new WrongCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
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

    @Transactional(readOnly = true)
    public UserAccountEntity getUserById(Integer userId) throws UserNotFoundException {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User account not found"));
    }

    @Transactional
    public void deleteAccount(Integer userId) throws UserNotFoundException {
        UserAccountEntity user = getUserById(userId);
        List<DataSourceEntity> ownedDatasources = dataSourceRepository.findAllByUserAccount_Id(userId);

        jwtService.revokeAllUserTokens(user);
        jwtService.deleteAllUserTokens(user);
        for (DataSourceEntity ownedDatasource : ownedDatasources) {
            dataSourceAccessRepository.deleteAllByDataSource_Id(ownedDatasource.getId());
            schemaRegistryService.evict(ownedDatasource.getId());
        }
        dataSourceRepository.deleteAllByUserAccount_Id(userId);
        dataSourceAccessRepository.deleteAllByUser_Id(userId);
        userAccountRepository.delete(user);
    }
}
