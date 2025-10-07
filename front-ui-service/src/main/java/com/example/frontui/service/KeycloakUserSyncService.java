package com.example.frontui.service;

import com.example.clients.customer.CustomerClient;
import com.example.clients.customer.CustomerRegistrationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserSyncService {

    private final CustomerClient customerClient;

    /**
     * Synchronizes the OIDC user with the Customer service.
     *
     * Parses fullname into firstName and lastName for Keycloak usage.
     *
     * @param oidcUser user from Keycloak
     * @return same OidcUser after synchronization
     */
    public OidcUser syncUser(OidcUser oidcUser) {
        var username = oidcUser.getPreferredUsername();
        var email = oidcUser.getEmail();

        // Получаем полное имя из claim "name" или getFullName()
        String fullName = oidcUser.getClaims().containsKey("name")
                ? oidcUser.getClaim("name")
                : oidcUser.getFullName();

        if (fullName == null || fullName.isBlank()) {
            fullName = username;
        }

        log.info("Attempt to synchronize User: username={}, email={}, fullName={}", username, email, fullName);

        // get firstName and lastName
        String firstName;
        String lastName;

        if (fullName.contains(" ")) {
            int index = fullName.indexOf(" ");
            firstName = fullName.substring(0, index).trim();
            lastName = fullName.substring(index + 1).trim();
        } else {
            firstName = fullName.trim();
            lastName = "";
        }

        try {
            // Извлекаем дату рождения из claim "birthdate"
            LocalDate birthdate = null;
            Object b = oidcUser.getClaims().get("birthdate");
            if (b != null) {
                birthdate = LocalDate.parse(b.toString());
            }

            // preparing request for Customer service
            var request = new CustomerRegistrationRequest(
                username,
                null,   // пароль в OAuth2 не нужен
                null,
                fullName,
                email,
                birthdate
            );

            var response = customerClient.registerCustomer(request);

            if (response.success()) {
                log.info("User synchronized successfully: {}", username);
            } else {
                log.warn("User sync returned errors for {}: {}", username, response.errors());
            }
        } catch (Exception ex) {
            log.error("Failed to synchronize user {}: {}", username, ex.getMessage(), ex);
        }

        return oidcUser;
    }
}
