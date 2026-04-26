package com.example.persistence.model;

import com.example.persistence.Enums.DataSourceAccessRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "data_source_accesses",
        indexes = {
                @Index(name = "idx_data_source_accesses_user_id", columnList = "user_id"),
                @Index(name = "idx_data_source_accesses_datasource_id", columnList = "data_source_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_data_source_access_user_data_source",
                columnNames = {"user_id", "data_source_id"}
        )
)
@Getter
@Setter
public class DataSourceAccessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_source_id", nullable = false)
    private DataSourceEntity dataSource;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DataSourceAccessRole accessRole;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}
