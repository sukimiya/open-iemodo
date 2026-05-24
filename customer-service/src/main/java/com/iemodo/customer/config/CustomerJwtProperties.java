package com.iemodo.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "iemodo.customer.jwt")
public class CustomerJwtProperties {

    private String privateKeyPath = "classpath:jwt/customer-private.pem";
    private String publicKeyPath  = "classpath:jwt/customer-public.pem";
    private long   accessTokenTtlMinutes  = 60;
    private long   refreshTokenTtlDays    = 30;
    private String issuer = "iemodo-customer-service";
}
