package com.iemodo.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI configuration for automatic API documentation generation.
 * 
 * <p>Access Swagger UI at: <a href="http://localhost:{port}/swagger-ui.html">...</a>
 * <p>Access OpenAPI JSON at: <a href="http://localhost:{port}/v3/api-docs">...</a>
 *
 * <p>Note: Swagger endpoints are whitelisted in {@link com.iemodo.common.tenant.TenantIdWebFilter}
 * to bypass the mandatory X-TenantID header check.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:iemodo-service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .description("iemodo 跨境电商微服务系统 API 文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("iemodo Team")
                                .email("support@iemodo.com")
                                .url("https://iemodo.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ));
    }
}
