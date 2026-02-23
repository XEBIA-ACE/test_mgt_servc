package com.example.usermanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Authentication response containing JWT tokens")
public class AuthResponse {

    @JsonProperty("access_token")
    @Schema(description = "Short-lived JWT access token (Bearer)", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;

    @JsonProperty("refresh_token")
    @Schema(description = "Long-lived opaque refresh token", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;

    @JsonProperty("token_type")
    @Builder.Default
    @Schema(description = "Token type, always 'Bearer'", example = "Bearer")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    @Schema(description = "Access token lifetime in seconds", example = "900")
    private long expiresIn;

    @Schema(description = "Authenticated user summary")
    private UserResponse user;
}
