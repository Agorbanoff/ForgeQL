package com.example.persistence.repository;

import com.example.persistence.Enums.DataSourceAccessRole;
import com.example.persistence.model.DataSourceAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceAccessRepository extends JpaRepository<DataSourceAccessEntity, Integer> {
    boolean existsByUser_IdAndDataSource_IdAndAccessRoleIn(
            Integer userId,
            Integer dataSourceId,
            Iterable<DataSourceAccessRole> accessRoles
    );

    boolean existsByUser_IdAndDataSource_Id(Integer userId, Integer dataSourceId);

    Optional<DataSourceAccessEntity> findByUser_IdAndDataSource_Id(Integer userId, Integer dataSourceId);

    List<DataSourceAccessEntity> findAllByDataSource_IdOrderByCreatedAtAsc(Integer dataSourceId);

    void deleteAllByDataSource_Id(Integer dataSourceId);

    void deleteAllByUser_Id(Integer userId);
}
