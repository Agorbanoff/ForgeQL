package com.example.persistence.model;

import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.Enums.SslMode;
import com.example.persistence.Enums.DataSourceStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(
        name = "data_sources",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_data_source_identity",
                columnNames = {"user_id", "db_type", "host", "port", "database_name", "schema_name", "username"}
        )
)
public class DataSourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DatabaseTypes dbType;

    @Column(nullable = false, length = 255)
    private String host;

    @Column(nullable = false)
    private Integer port;

    // client's database name in their system
    @Column(nullable = false, length = 255)
    private String databaseName;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, columnDefinition = "text")
    private String encryptedPassword;

    @Column(name = "schema_name", nullable = false, length = 255)
    private String schemaName;

    @Column(name = "ssl_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private SslMode sslMode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DataSourceStatus status;

    @Column(name = "connect_timeout_ms")
    private Integer connectTimeoutMs;

    @Column(name = "socket_timeout_ms")
    private Integer socketTimeoutMs;

    @Column(name = "application_name", length = 255)
    private String applicationName;

    @Column(name = "ssl_root_cert_ref", length = 512)
    private String sslRootCertRef;

    @Column(name = "extra_jdbc_options_json", columnDefinition = "text")
    private String extraJdbcOptionsJson;

    @Column(name = "last_connection_test_at")
    private Instant lastConnectionTestAt;

    @Column(name = "last_connection_status")
    @Enumerated(EnumType.STRING)
    private DataSourceConnectionStatus lastConnectionStatus;

    @Column(name = "last_connection_error", columnDefinition = "text")
    private String lastConnectionError;

    @Column(name = "last_schema_generated_at")
    private Instant lastSchemaGeneratedAt;

    @Column(name = "last_schema_fingerprint", length = 255)
    private String lastSchemaFingerprint;

    @Column(name = "server_version", length = 255)
    private String serverVersion;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccountEntity userAccount;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
