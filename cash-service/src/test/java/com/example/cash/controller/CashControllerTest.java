package com.example.cash.controller;

import com.example.cash.service.CashService;
import com.example.clients.cash.CashAction;
import com.example.clients.cash.CashOperationRequest;
import com.example.clients.cash.CashOperationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CashController.class)
@TestPropertySource(properties = {
        "spring.profiles.active=default"
})
class CashControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CashService cashService;

    @Test
    void testProcessOperation() throws Exception {
        var req = new CashOperationRequest(
                "user5", "RUB",
                BigDecimal.valueOf(50), CashAction.DEPOSIT);

        var okResponse = CashOperationResponse.success();
        when(cashService.processCashOperation(any())).thenReturn(okResponse);

        mockMvc.perform(post("/api/v1/cash/operation")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "user5")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(cashService).processCashOperation(req);
    }

    @Test
    void testProcessOperation_forbidden() throws Exception {
        var req = new CashOperationRequest(
                "unknown", "USD",
                BigDecimal.ONE, CashAction.DEPOSIT);

        mockMvc.perform(post("/api/v1/cash/operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isForbidden());
    }
}
