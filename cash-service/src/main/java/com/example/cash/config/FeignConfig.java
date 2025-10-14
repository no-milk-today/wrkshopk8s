package com.example.cash.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

// https://www.edstem.com/blog/jwt-tokens-microservice-architectures-spring-cloud/
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor relayJwt() {
        return template -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                template.header(
                        "Authorization",
                        "Bearer " + jwtAuth.getToken().getTokenValue()
                );
            }
        };
    }
}

