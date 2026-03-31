package com.example.controller.dtos.response;

import com.example.persistence.Enums.DatabaseTypes;
import com.example.persistence.Enums.SslMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResDataSourceDTO {
    private Integer id;
    private String name;
    private DatabaseTypes dbType;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String schemaName;
    private Boolean SslEnabled;
    private SslMode sslMode;
}
