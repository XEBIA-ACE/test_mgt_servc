package com.example.usermanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login credentials")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(description = "Username or email address", example = "jane_doe")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password")
    private String password;
}
