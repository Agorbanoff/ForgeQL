package com.example;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.persistence.repository",
        entityManagerFactoryRef = "appEntityManagerFactory",
        transactionManagerRef = "appTransactionManager"
)
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource")
    public DataSourceProperties appDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("app.datasource.hikari")
    public DataSource appDataSource() {
        return appDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean appEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            JpaProperties jpaProperties,
            @Qualifier("appDataSource") DataSource appDataSource
    ) {
        Map<String, Object> properties = new HashMap<>(jpaProperties.getProperties());
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        return builder
                .dataSource(appDataSource)
                .packages("com.example.persistence.model")
                .persistenceUnit("app")
                .properties(properties)
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager appTransactionManager(
            @Qualifier("appEntityManagerFactory") LocalContainerEntityManagerFactoryBean appEntityManagerFactory
    ) {
        return new JpaTransactionManager(appEntityManagerFactory.getObject());
    }
}
