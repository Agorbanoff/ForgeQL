package com.example.controller.dtos.request;

import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.Enums.SslMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDataSourceDTO(
        @NotBlank
        @Size(max = 255)
        String displayName,

        @NotNull
        DatabaseTypes dbType,

        @NotBlank
        @Size(max = 255)
        String host,

        @NotNull
        @Min(1)
        @Max(65535)
        Integer port,

        @NotBlank
        @Size(max = 255)
        String databaseName,

        @NotBlank
        @Size(max = 255)
        String schemaName,

        @NotBlank
        @Size(max = 255)
        String username,

        String password,

        @NotNull
        SslMode sslMode,

        @Min(1)
        Integer connectTimeoutMs,

        @Min(1)
        Integer socketTimeoutMs,

        @Size(max = 255)
        String applicationName,

        @Size(max = 512)
        String sslRootCertRef,

        String extraJdbcOptionsJson
) {
}
