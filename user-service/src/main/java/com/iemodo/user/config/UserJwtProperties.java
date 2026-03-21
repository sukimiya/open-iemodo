package com.iemodo.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "iemodo.user.jwt")
public class UserJwtProperties {

    private String privateKeyPath = "classpath:jwt/private.pem";
    private String publicKeyPath  = "classpath:jwt/public.pem";
    private long   accessTokenTtlMinutes  = 60;
    private long   refreshTokenTtlDays    = 30;
    private String issuer = "iemodo-user-service";
}
