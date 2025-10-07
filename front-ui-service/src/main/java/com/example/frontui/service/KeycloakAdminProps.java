package com.example.frontui.service;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProps {

    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;


    public String adminUri(String path) {
        return String.format("%s/admin/realms/%s%s", serverUrl, realm, path);
    }
}


