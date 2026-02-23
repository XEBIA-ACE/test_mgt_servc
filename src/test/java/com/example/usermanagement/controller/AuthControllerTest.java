package com.example.usermanagement.controller;

import com.example.usermanagement.dto.request.LoginRequest;
import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.response.AuthResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register returns 201 on success")
    @WithMockUser
    void register_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("jane@example.com");
        req.setUsername("jane_doe");
        req.setPassword("Secret@123");

        UserResponse response = UserResponse.builder()
            .id(UUID.randomUUID())
            .email("jane@example.com")
            .username("jane_doe")
            .build();
        when(authService.register(any())).thenReturn(response);

        mvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("jane_doe"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register returns 400 when email is invalid")
    @WithMockUser
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("not-an-email");
        req.setUsername("jane_doe");
        req.setPassword("Secret@123");

        mvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login returns 200 with tokens on success")
    @WithMockUser
    void login_returns200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("jane_doe");
        req.setPassword("Secret@123");

        AuthResponse authResponse = AuthResponse.builder()
            .accessToken("access.jwt.token")
            .refreshToken("refresh-uuid")
            .expiresIn(900)
            .build();
        when(authService.login(any())).thenReturn(authResponse);

        mvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.access_token").value("access.jwt.token"));
    }
}
