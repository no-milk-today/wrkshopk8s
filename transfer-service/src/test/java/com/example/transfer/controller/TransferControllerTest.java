package com.example.transfer.controller;

import com.example.clients.transfer.TransferRequest;
import com.example.clients.transfer.TransferResponse;
import com.example.transfer.service.TransferService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TransferController.class)
@TestPropertySource(properties = {
        "spring.profiles.active=default"
})
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TransferService transferService;

    @Test
    void transfer_success() throws Exception {
        var req = new TransferRequest(
                "RUB", "RUB",
                BigDecimal.valueOf(123.45),
                "user5"
        );
        var okResp = TransferResponse.success();
        when(transferService.transfer(eq("user7"), eq(req))).thenReturn(okResp);

        mockMvc.perform(post("/user/user7/transfer")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "user7")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)));

        verify(transferService).transfer("user7", req);
    }

    @Test
    void transfer_forbidden() throws Exception {
        var req = new TransferRequest(
                "RUB", "RUB",
                BigDecimal.ONE,
                "usertest"
        );

        mockMvc.perform(post("/user/user7/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isForbidden());
    }
}