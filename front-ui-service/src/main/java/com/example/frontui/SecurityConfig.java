package com.example.frontui;

import com.example.frontui.service.KeycloakUserSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final KeycloakUserSyncService userSyncService;

    /**
     * Bean для обработки загрузки OIDC-пользователя во время OAuth2 логина.
     *
     * Использует дефолтный OidcUserService для получения пользователя от провайдера,
     * а затем синхронизирует его с Customer сервисом через KeycloakUserSyncService.
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            // Загружаем пользователя от Keycloak
            OidcUser oidcUser = delegate.loadUser(userRequest);

            // Синхронизируем с Customer сервисом
            return userSyncService.syncUser(oidcUser);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutHandler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        // после выхода уходим на базовый URL клиента
        oidcLogoutHandler.setPostLogoutRedirectUri("{baseUrl}");

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth ->
                auth.requestMatchers("/signup/**", "/css/**", "/js/**", "/actuator/health").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2Login(oauth ->
                oauth
                    .loginPage("/oauth2/authorization/keycloak")
                    .userInfoEndpoint(user -> user.oidcUserService(oidcUserService()))
            )
            .logout(logout -> logout
                .logoutSuccessHandler(oidcLogoutHandler)
            );

        return http.build();
    }
}
