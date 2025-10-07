package com.example.frontui;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor oauth2TokenRelayInterceptor(
            OAuth2AuthorizedClientService clientService) {

        return requestTemplate -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof OAuth2AuthenticationToken oauth2Auth) {
                // registrationId = "keycloak" по умолчанию
                OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                        oauth2Auth.getAuthorizedClientRegistrationId(),
                        oauth2Auth.getName());

                if (client != null && client.getAccessToken() != null) {
                    String tokenValue = client.getAccessToken().getTokenValue();
                    requestTemplate.header("Authorization", "Bearer " + tokenValue);
                }
            }
        };
    }
}


