package com.example.SigmaQL.persistence.model;

import com.example.SigmaQL.persistence.Enums.DatabaseTypes;
import com.example.SigmaQL.persistence.Enums.SslMode;
import com.example.SigmaQL.persistence.Enums.DataSourceStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Data
@Table(name = "databases")
public class DataSourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // the name that client gives to their database so that we can call it that way
    @Column(nullable = false, length = 255)
    private String name;

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

    @Column(nullable = false)
    private String encryptedPassword;

    @Column(name = "schema_name", length = 255)
    private String schemaName;

    @Column(nullable = false)
    private boolean sslEnabled;

    @Column()
    @Enumerated(EnumType.STRING)
    private SslMode sslMode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DataSourceStatus status;

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