package com.example.usermanagement.controller;

import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.model.entity.Role;
import com.example.usermanagement.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserController integration tests")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;

    @Test
    @DisplayName("POST /api/v1/users/register - creates user and returns 201")
    void register_validRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("johndoe");
        request.setEmail("john@example.com");
        request.setPassword("Secret@1");

        UserResponse created = UserResponse.builder()
            .id(UUID.randomUUID())
            .username("johndoe")
            .email("john@example.com")
            .roles(Set.of(Role.ROLE_USER))
            .build();

        given(userService.register(any(RegisterRequest.class))).willReturn(created);

        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.username").value("johndoe"));
    }

    @Test
    @DisplayName("GET /api/v1/users - unauthenticated returns 401")
    void listUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/users - authenticated non-admin returns 403")
    @WithMockUser(roles = "USER")
    void listUsers_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/users/me - authenticated user returns own profile")
    @WithMockUser(username = "john", roles = "USER")
    void getMe_authenticated_returnsProfile() throws Exception {
        // /me resolves from the security context — controller test with @WithMockUser
        // requires UserPrincipal; this validates the route is accessible.
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk());
    }
}
