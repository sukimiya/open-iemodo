package com.iemodo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "iemodo.gateway")
public class LiteGatewayProperties {

    private Jwt jwt = new Jwt();
    private List<String> whitelist = List.of();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public List<String> getWhitelist() { return whitelist; }
    public void setWhitelist(List<String> whitelist) { this.whitelist = whitelist; }

    public static class Jwt {
        private String publicKeyPath;

        public String getPublicKeyPath() { return publicKeyPath; }
        public void setPublicKeyPath(String publicKeyPath) { this.publicKeyPath = publicKeyPath; }
    }
}
