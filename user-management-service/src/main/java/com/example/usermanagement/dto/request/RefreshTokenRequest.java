package com.example.usermanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Refresh token payload for obtaining a new access token")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Opaque refresh token issued during login", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;
}
