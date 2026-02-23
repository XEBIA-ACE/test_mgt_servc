package com.example.usermanagement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Authentication response containing tokens")
public class AuthResponse {

    @Schema(description = "JWT access token")
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "JWT refresh token")
    @JsonProperty("refresh_token")
    private String refreshToken;

    @Schema(description = "Token type (always 'Bearer')", example = "Bearer")
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token validity in seconds")
    @JsonProperty("expires_in")
    private long expiresIn;

    @Schema(description = "Authenticated user info")
    private UserResponse user;
}
