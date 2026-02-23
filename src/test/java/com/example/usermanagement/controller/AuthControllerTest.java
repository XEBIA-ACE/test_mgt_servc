package com.example.usermanagement.controller;

import com.example.usermanagement.dto.request.LoginRequest;
import com.example.usermanagement.dto.response.AuthResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController integration tests")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/login - valid credentials return 200 with tokens")
    void login_validCredentials_returnsTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john@example.com");
        request.setPassword("Secret@1");

        AuthResponse response = AuthResponse.builder()
            .accessToken("access-token")
            .refreshToken("refresh-token")
            .expiresIn(900)
            .user(UserResponse.builder().id(UUID.randomUUID()).username("john").build())
            .build();

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.access_token").value("access-token"))
            .andExpect(jsonPath("$.data.refresh_token").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - blank credentials return 400")
    void login_blankCredentials_returns400() throws Exception {
        LoginRequest request = new LoginRequest(); // missing fields

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
