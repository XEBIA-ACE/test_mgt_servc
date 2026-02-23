package com.example.usermanagement.controller;

import com.example.usermanagement.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Service liveness probe")
public class HealthController {

    @GetMapping("/ping")
    @Operation(summary = "Simple liveness check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "status", "UP",
            "service", "user-management-service",
            "timestamp", Instant.now()
        )));
    }
}
