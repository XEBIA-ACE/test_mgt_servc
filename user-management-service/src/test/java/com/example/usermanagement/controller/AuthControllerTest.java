package com.example.usermanagement.controller;

import com.example.usermanagement.exception.EmailAlreadyExistsException;
import com.example.usermanagement.model.dto.request.LoginRequest;
import com.example.usermanagement.model.dto.request.RefreshTokenRequest;
import com.example.usermanagement.model.dto.request.RegisterRequest;
import com.example.usermanagement.model.dto.response.AuthResponse;
import com.example.usermanagement.model.dto.response.UserResponse;
import com.example.usermanagement.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // ===== Register =====

    @Test
    @DisplayName("POST /auth/register — valid request returns 201 with tokens")
    void register_validRequest_returns201() throws Exception {
        RegisterRequest request = buildRegisterRequest();
        AuthResponse authResponse = buildAuthResponse();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.access_token").value("test-access-token"))
            .andExpect(jsonPath("$.data.token_type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register — invalid email returns 400")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = buildRegisterRequest();
        request.setEmail("not-an-email");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /auth/register — weak password returns 400")
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest request = buildRegisterRequest();
        request.setPassword("weak");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/register — duplicate email returns 409")
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("Email already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRegisterRequest())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ===== Login =====

    @Test
    @DisplayName("POST /auth/login — valid credentials returns 200 with tokens")
    void login_validCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Test@1234");

        when(authService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.access_token").value("test-access-token"));
    }

    @Test
    @DisplayName("POST /auth/login — bad credentials returns 401")
    void login_badCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("WrongPass@1");

        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /auth/login — missing email returns 400")
    void login_missingEmail_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPassword("Test@1234");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ===== Refresh =====

    @Test
    @DisplayName("POST /auth/refresh — valid token returns new access token")
    void refresh_validToken_returns200() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(authService.refreshToken(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.access_token").value("test-access-token"));
    }

    // ===== Helpers =====

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("Test@1234");
        req.setFirstName("Test");
        req.setLastName("User");
        return req;
    }

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
            .accessToken("test-access-token")
            .refreshToken("test-refresh-token")
            .expiresIn(900)
            .user(UserResponse.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .build())
            .build();
    }
}
