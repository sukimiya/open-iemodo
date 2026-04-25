package com.iemodo.user.config;

import com.iemodo.common.tenant.MultitenantR2dbcConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Enables multi-tenant R2DBC repositories for user-service.
 * Imports the shared {@link MultitenantR2dbcConfiguration} bean definitions
 * rather than extending the class, to avoid Spring Boot auto-config conflicts.
 */
@Configuration
@Import(MultitenantR2dbcConfiguration.class)
public class UserR2dbcConfig {
}
