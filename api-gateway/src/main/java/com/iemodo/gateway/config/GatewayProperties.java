package com.iemodo.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Gateway-specific configuration properties loaded from
 * {@code application.yml} / Nacos under the {@code iemodo.gateway} prefix.
 */
@Data
@Component
@ConfigurationProperties(prefix = "iemodo.gateway")
public class GatewayProperties {

    private Jwt jwt = new Jwt();

    /**
     * Whitelist entries — format: {@code METHOD:/path/pattern}
     * e.g. {@code POST:/uc/api/v1/auth/login}
     * Supports Ant-style wildcards: {@code /**}
     */
    private List<String> whitelist = new ArrayList<>();

    @Data
    public static class Jwt {
        /** Classpath or file-system path to the RSA public key PEM file. */
        private String publicKeyPath = "classpath:jwt/public.pem";
    }
}
