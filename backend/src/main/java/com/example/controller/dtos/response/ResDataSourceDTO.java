package com.example.controller.dtos.response;

import com.example.persistence.Enums.DataSourceConnectionStatus;
import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.Enums.DataSourceStatus;
import com.example.persistence.Enums.SslMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResDataSourceDTO {
    private Integer id;
    private Integer ownerUserId;
    private String displayName;
    private DatabaseTypes dbType;
    private String host;
    private Integer port;
    private String databaseName;
    private String schemaName;
    private String username;
    private SslMode sslMode;
    private Integer connectTimeoutMs;
    private Integer socketTimeoutMs;
    private String applicationName;
    private String sslRootCertRef;
    private String extraJdbcOptionsJson;
    private DataSourceStatus status;
    private Instant lastConnectionTestAt;
    private DataSourceConnectionStatus lastConnectionStatus;
    private String lastConnectionError;
    private Instant lastSchemaGeneratedAt;
    private String lastSchemaFingerprint;
    private String serverVersion;
    private Instant createdAt;
    private Instant updatedAt;
}
