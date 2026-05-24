package com.iemodo.customer.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(prefix = "iemodo.deployment", name = "mode", havingValue = "microservices")
public class CustomerSecurityConfig {

    @Bean
    public PasswordEncoder customerPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
