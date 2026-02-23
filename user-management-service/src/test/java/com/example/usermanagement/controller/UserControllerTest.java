package com.example.usermanagement.controller;

import com.example.usermanagement.model.dto.response.PagedResponse;
import com.example.usermanagement.model.dto.response.UserResponse;
import com.example.usermanagement.model.enums.Role;
import com.example.usermanagement.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("User Controller Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // ===== Access control tests =====

    @Test
    @DisplayName("GET /admin/users — unauthenticated request returns 401")
    void getAllUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("GET /admin/users — USER role returns 403")
    void getAllUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("GET /admin/users — ADMIN role returns 200")
    void getAllUsers_asAdmin_returns200() throws Exception {
        PagedResponse<UserResponse> pagedResponse = PagedResponse.<UserResponse>builder()
            .content(List.of(buildUserResponse()))
            .page(0)
            .size(20)
            .totalElements(1)
            .totalPages(1)
            .first(true)
            .last(true)
            .build();

        when(userService.getAllUsers(anyInt(), anyInt(), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("GET /admin/users/:id — returns user when found")
    void getUserById_asAdmin_returnsUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = buildUserResponse();
        userResponse = UserResponse.builder()
            .id(userId)
            .username("testuser")
            .email("test@example.com")
            .role(Role.USER)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .build();

        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/admin/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @DisplayName("DELETE /admin/users/:id — returns 200 on success")
    void deleteUser_asAdmin_returns200() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/users/{id}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ===== Helper =====

    private UserResponse buildUserResponse() {
        return UserResponse.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .role(Role.USER)
            .enabled(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
