package com.example.persistence.repository;

import com.example.persistence.model.JwtEntity;
import com.example.persistence.model.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JwtRepository extends JpaRepository<JwtEntity, Integer> {
    Optional<JwtEntity> findByJti(String jti);
    List<JwtEntity> findAllByUserAccountAndRevokedFalse(UserAccountEntity userAccount);
    void deleteAllByUserAccount(UserAccountEntity userAccount);
    void deleteAllByExpiresAtBefore(Instant instant);
}