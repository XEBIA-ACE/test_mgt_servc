package com.example.usermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight liveness probe for load balancers and orchestration platforms.
 * Detailed readiness info is served by Spring Actuator at /actuator/health.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Service health check")
public class HealthController {

    @GetMapping("/ping")
    @Operation(summary = "Liveness check — returns pong with server timestamp")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        ));
    }
}
