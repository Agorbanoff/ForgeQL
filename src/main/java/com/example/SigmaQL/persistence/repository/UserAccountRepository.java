package com.example.SigmaQL.persistence.repository;

import com.example.SigmaQL.persistence.model.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Integer> {
    boolean existsByEmail(String email);
    Optional<UserAccountEntity> findByEmail(String email);
}