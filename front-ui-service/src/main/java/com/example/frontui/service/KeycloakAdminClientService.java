package com.example.frontui.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminClientService {

    private final KeycloakAdminProps props;
    private final WebClient webClient = WebClient.builder().build();
    private String cachedToken;   // very simple caching (30 s)

    /** Obtain service-account token (client-credentials). */
    private Mono<String> getToken() {
        if (cachedToken != null) return Mono.just(cachedToken);
        return webClient.post()
                .uri(props.getServerUrl() + "/realms/" + props.getRealm() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("grant_type", "client_credentials")
                        .with("client_id", props.getClientId())
                        .with("client_secret", props.getClientSecret()))
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> (String) body.get("access_token"))
                .doOnNext(t -> cachedToken = t);
    }

    /** Create user + set password (enabled). */
    public Mono<String> createUser(String username,
                                   String password,
                                   String email,
                                   String fullName) {

        // Разбиваем fullName на firstName и lastName
        String firstName;
        String lastName;
        if (fullName != null && fullName.contains(" ")) {
            int i = fullName.indexOf(' ');
            firstName = fullName.substring(0, i).trim();
            lastName = fullName.substring(i + 1).trim();
        } else {
            firstName = fullName != null ? fullName : "";
            lastName = "";
        }

        var userPayload = Map.of(
                "username", username,
                "enabled", true,
                "email", email,
                "firstName", firstName,
                "lastName", lastName
        );

        return getToken().flatMap(token ->
                webClient.post()
                        .uri(props.adminUri("/users"))
                        .headers(h -> h.setBearerAuth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(userPayload)
                        .exchangeToMono(resp -> {
                            if (resp.statusCode().is2xxSuccessful()) {
                                String location = resp.headers().asHttpHeaders().getFirst("Location");
                                return Mono.just(location.substring(location.lastIndexOf('/') + 1));
                            }
                            return resp.bodyToMono(String.class)
                                    .flatMap(err -> Mono.error(new IllegalStateException(err)));
                        })
                        .flatMap(userId -> setPassword(token, userId, password).thenReturn(userId))
        );
    }


    private Mono<Void> setPassword(String token, String userId, String password) {
        var cred = Map.of(
                "type", "password",
                "value", password,
                "temporary", false);

        return webClient.put()
                .uri(props.adminUri("/users/" + userId + "/reset-password"))
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(cred)
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}
