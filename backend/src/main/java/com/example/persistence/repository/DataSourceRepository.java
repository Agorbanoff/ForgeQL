package com.example.persistence.repository;

import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.model.DataSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSourceEntity, Integer> {
    Optional<DataSourceEntity> findByIdAndUserAccount_Id(Integer id, Integer userId);
    List<DataSourceEntity> findAllByUserAccount_Id(Integer userId);
    boolean existsByUserAccountIdAndDbTypeAndHostAndPortAndDatabaseNameAndUsername(
            Integer userId,
            DatabaseTypes dbType,
            String host,
            Integer port,
            String databaseName,
            String username
    );
    boolean existsByUserAccountIdAndDbTypeAndHostAndPortAndDatabaseNameAndUsernameAndIdNot(
            Integer userId,
            DatabaseTypes dbType,
            String host,
            Integer port,
            String databaseName,
            String username,
            Integer id
    );
}
