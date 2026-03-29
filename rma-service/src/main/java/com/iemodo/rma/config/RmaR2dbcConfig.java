package com.iemodo.rma.config;

import com.iemodo.common.tenant.MultitenantR2dbcConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@Import(MultitenantR2dbcConfiguration.class)
@EnableR2dbcRepositories(basePackages = "com.iemodo.rma.repository")
public class RmaR2dbcConfig {
}
