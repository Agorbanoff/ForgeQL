package com.example.persistence.repository;

import com.example.persistence.Enums.GlobalRole;
import com.example.persistence.model.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Integer> {
    java.util.List<UserAccountEntity> findAllByOrderByUsernameAsc();
    boolean existsByGlobalRole(GlobalRole globalRole);
    long countByGlobalRole(GlobalRole globalRole);
    boolean existsByEmail(String email);
    Optional<UserAccountEntity> findByEmail(String email);
}
