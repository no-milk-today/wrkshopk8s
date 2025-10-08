package com.example.customerservice.controller;

import com.example.clients.customer.CustomerRegistrationRequest;
import com.example.clients.customer.CustomerRegistrationResponse;
import com.example.clients.customer.MainPageData;
import com.example.customerservice.config.SecurityConfig;
import com.example.customerservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@TestPropertySource(properties = {
        "spring.profiles.active=default"
})
class CustomerControllerTest {

    private static final String TEST_USERNAME = "john";
    private static final String TEST_EMAIL = "john@example.com";
    private static final LocalDate TEST_BIRTHDATE = LocalDate.of(1990, 1, 1);

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private final JwtDecoder jwtDecoder;
    @MockitoBean
    private final CustomerService customerService;

    private final Jwt testJwt = createTestJwt(TEST_USERNAME);

    private Jwt createTestJwt(String username) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @BeforeEach
    void setupJwtDecoder() {
        when(jwtDecoder.decode(anyString())).thenReturn(testJwt);
    }

    @Test
    @DisplayName("POST /api/v1/customers/signup returns JSON registration response")
    void registerCustomer_returnsJson() throws Exception {
        var req = new CustomerRegistrationRequest(
                TEST_USERNAME, "pass", "pass", "John Doe", TEST_EMAIL, TEST_BIRTHDATE
        );
        var resp = new CustomerRegistrationResponse(true, List.of());
        when(customerService.registerCustomer(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/customers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));

        verify(customerService).registerCustomer(req);
    }

    @Test
    @DisplayName("GET /api/v1/customers/public is accessible without auth")
    void publicEndpoint_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/customers/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("Customers public endpoint"));
    }

    @Test
    @DisplayName("GET /api/v1/customers/main requires auth and invokes service")
    void main_requiresJwt_andInvokesService() throws Exception {
        var mainData = createTestMainPageData(TEST_USERNAME, "John Doe", TEST_EMAIL);
        when(customerService.getMainData(TEST_USERNAME)).thenReturn(mainData);

        mockMvc.perform(get("/api/v1/customers/main")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", TEST_USERNAME)))
                        .param("login", TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(TEST_USERNAME))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(customerService).getMainData(TEST_USERNAME);
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{login}/accounts/{currency}/balance updates balance")
    void updateAccountBalance_requiresJwt() throws Exception {
        var newBalance = BigDecimal.valueOf(500);

        mockMvc.perform(put("/api/v1/customers/" + TEST_USERNAME + "/accounts/USD/balance")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", TEST_USERNAME)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(newBalance)))
                .andExpect(status().isOk());

        verify(customerService).updateAccountBalance(TEST_USERNAME, "USD", newBalance);
    }

    // утилитный метод для создания тестовых MainPageData
    private MainPageData createTestMainPageData(String login, String name, String email) {
        return new MainPageData(
                login,
                name,
                email,
                TEST_BIRTHDATE,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}