package com.example.usermanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Partial update request — all fields are optional.
 * Only non-null fields will be applied to the existing user record.
 */
@Data
@Schema(description = "User profile update payload (all fields optional)")
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username may only contain letters, digits, underscores, dots, and hyphens")
    @Schema(description = "New username", example = "john_doe_updated")
    private String username;

    @Email(message = "Email must be a valid address")
    @Size(max = 255)
    @Schema(description = "New email address", example = "john.new@example.com")
    private String email;

    @Size(max = 100)
    @Schema(description = "First name", example = "Jonathan")
    private String firstName;

    @Size(max = 100)
    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Enable or disable the account")
    private Boolean enabled;
}
