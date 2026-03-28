package com.iemodo.gateway.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * R2DBC configuration for API Gateway.
 * 
 * <p>Connects to the gateway_config schema for dynamic route and rate limit configuration.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.iemodo.gateway.repository")
public class GatewayR2dbcConfig extends AbstractR2dbcConfiguration {

    @Override
    public ConnectionFactory connectionFactory() {
        // Connection factory is auto-configured by Spring Boot from application.yml
        // This method is required by AbstractR2dbcConfiguration
        return null;
    }

    /**
     * Initialize the database schema on startup.
     */
    @Bean
    public ConnectionFactoryInitializer initializer(@Qualifier("connectionFactory") ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        
        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
        populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__create_gateway_tables.sql")));
        initializer.setDatabasePopulator(populator);
        
        return initializer;
    }
}
