package com.example.usermanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@Schema(description = "User profile response")
public class UserResponse {

    @Schema(description = "User ID (UUID)")
    private UUID id;

    @Schema(description = "Email address")
    private String email;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "First name")
    @JsonProperty("first_name")
    private String firstName;

    @Schema(description = "Last name")
    @JsonProperty("last_name")
    private String lastName;

    @Schema(description = "Whether the account is active")
    private boolean enabled;

    @Schema(description = "Whether the email address has been verified")
    @JsonProperty("email_verified")
    private boolean emailVerified;

    @Schema(description = "Assigned roles")
    private Set<String> roles;

    @Schema(description = "Timestamp of last successful login")
    @JsonProperty("last_login_at")
    private Instant lastLoginAt;

    @Schema(description = "Account creation timestamp")
    @JsonProperty("created_at")
    private Instant createdAt;

    @Schema(description = "Last profile update timestamp")
    @JsonProperty("updated_at")
    private Instant updatedAt;
}
