package com.example.usermanagement.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Uniform envelope for all API responses.
 *
 * <pre>
 * {
 *   "success": true,
 *   "message": "...",
 *   "data": { ... },
 *   "errors": null,
 *   "timestamp": "2024-01-01T00:00:00"
 * }
 * </pre>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Object errors;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ===== Factory helpers =====

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().success(false).message(message).build();
    }

    public static <T> ApiResponse<T> error(String message, Object errors) {
        return ApiResponse.<T>builder().success(false).message(message).errors(errors).build();
    }
}
