package com.example.usermanagement.controller;

import com.example.usermanagement.dto.request.ChangePasswordRequest;
import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.request.UpdateUserRequest;
import com.example.usermanagement.dto.response.ApiResponse;
import com.example.usermanagement.dto.response.PageResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.model.entity.Role;
import com.example.usermanagement.security.UserPrincipal;
import com.example.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    // ── Registration (public) ─────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("User registered successfully", created));
    }

    // ── Current user ──────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(principal.getId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(principal.getId(), request)));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Change the currently authenticated user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
            Sort.Direction.fromString(direction), sort);
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers(pageable)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users by name, username, or email (admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.ok(userService.searchUsers(q, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> getById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update any user's profile (admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request)));
    }

    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a role to a user (admin only)")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable UUID id,
            @PathVariable Role role) {
        userService.assignRole(id, role);
        return ResponseEntity.ok(ApiResponse.ok("Role assigned", null));
    }

    @DeleteMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a role from a user (admin only)")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable UUID id,
            @PathVariable Role role) {
        userService.removeRole(id, role);
        return ResponseEntity.ok(ApiResponse.ok("Role removed", null));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable a user account (admin only)")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable UUID id) {
        userService.disableUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User disabled", null));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable a user account (admin only)")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable UUID id) {
        userService.enableUser(id);
        return ResponseEntity.ok(ApiResponse.ok("User enabled", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete a user (admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
