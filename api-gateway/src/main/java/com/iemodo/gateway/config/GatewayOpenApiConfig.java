package com.iemodo.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayList;
import java.util.List;

/**
 * Gateway OpenAPI aggregation configuration.
 * 
 * <p>Aggregates OpenAPI docs from all downstream services.
 * Access the unified Swagger UI at: <a href="http://localhost:8080/swagger-ui.html">...</a>
 *
 * <p>Note: Each service must expose its OpenAPI at /v3/api-docs
 * (automatically provided by springdoc-openapi).
 */
@Configuration
public class GatewayOpenApiConfig {

    @Autowired
    @Lazy
    private RouteDefinitionLocator locator;

    /**
     * Creates grouped OpenAPI definitions for each route.
     * Groups are shown in the Swagger UI dropdown.
     */
    @Bean
    @Lazy
    public List<GroupedOpenApi> apis() {
        List<GroupedOpenApi> groups = new ArrayList<>();
        
        List<RouteDefinition> definitions = locator.getRouteDefinitions()
                .collectList()
                .block();
        
        if (definitions != null) {
            definitions.stream()
                    .filter(route -> route.getId().endsWith("-route"))
                    .forEach(route -> {
                        String name = route.getId().replace("-route", "");
                        GroupedOpenApi api = GroupedOpenApi.builder()
                                .group(name)
                                .pathsToMatch("/" + name + "/**")
                                .build();
                        groups.add(api);
                    });
        }
        
        return groups;
    }
}
