package com.example.usermanagement.dto.response;

import com.example.usermanagement.model.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Public-facing user representation — never includes sensitive fields. */
@Data
@Builder
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Set<Role> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
