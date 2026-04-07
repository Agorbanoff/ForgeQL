package com.example.controller.dtos.request;

import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.Enums.SslMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReqDataSourceDTO {
    @NotBlank
    @Size(max = 255)
    private String displayName;

    @NotNull
    private DatabaseTypes dbType;

    @NotBlank
    @Size(max = 255)
    private String host;

    @NotNull
    @Min(1)
    @Max(65535)
    private Integer port;

    @NotBlank
    @Size(max = 255)
    private String databaseName;

    @NotBlank
    @Size(max = 255)
    private String schemaName;

    @NotBlank
    @Size(max = 255)
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private SslMode sslMode;

    @Min(1)
    private Integer connectTimeoutMs;

    @Min(1)
    private Integer socketTimeoutMs;

    @Size(max = 255)
    private String applicationName;

    @Size(max = 512)
    private String sslRootCertRef;

    private String extraJdbcOptionsJson;
}
