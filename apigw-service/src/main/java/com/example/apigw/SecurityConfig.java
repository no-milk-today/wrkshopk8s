package com.example.apigw;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        // всё что обслуживает Front-UI, пропускаем
                        .pathMatchers("/", "/main/**", "/signup/**", "/user/**",
                                "/css/**", "/js/**", "/webjars/**", "/favicon.ico", "/images/**",
                                "/oauth2/authorization/**", "/login/oauth2/code/**", "/logout", "/actuator/**")
                        .permitAll()
                        // public JS-endpoint курсов валют
                        .pathMatchers("/api/rates/**").permitAll()
                        // остальные REST-path → только с JWT
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }
}
