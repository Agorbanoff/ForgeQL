package com.example.SigmaQL.controller.dtos.request;

import com.example.SigmaQL.persistence.Enums.DataSourceStatus;
import com.example.SigmaQL.persistence.Enums.DatabaseTypes;
import com.example.SigmaQL.persistence.Enums.SslMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReqDataSourceDTO {
    @NotNull
    private String name;

    @NotNull
    private DatabaseTypes dbType;

    @NotNull
    private String host;

    @NotNull
    @Min(1)
    @Max(65535)
    private Integer port;

    @NotNull
    private String databaseName;

    @NotNull
    private String username;

    @NotNull
    private String encryptedPassword;

    private String schemaName;

    private Boolean SslEnabled;

    private SslMode sslMode;
}
