package com.example.usermanagement.controller;

import com.example.usermanagement.dto.response.ApiResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Users", description = "Admin-only user management operations")
public class AdminController {

    private final UserService userService;

    @PostMapping("/{id}/disable")
    @Operation(summary = "Disable a user account")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable UUID id) {
        userService.disableUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User disabled", null));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "Enable a (previously disabled or locked) user account")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable UUID id) {
        userService.enableUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User enabled", null));
    }

    @PostMapping("/{id}/roles/{roleName}")
    @Operation(summary = "Assign a role to a user")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
        @PathVariable UUID id,
        @PathVariable String roleName
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.assignRole(id, roleName)));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    @Operation(summary = "Remove a role from a user")
    public ResponseEntity<ApiResponse<UserResponse>> removeRole(
        @PathVariable UUID id,
        @PathVariable String roleName
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.removeRole(id, roleName)));
    }
}
