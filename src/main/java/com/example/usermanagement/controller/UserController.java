package com.example.usermanagement.controller;

import com.example.usermanagement.dto.request.ChangePasswordRequest;
import com.example.usermanagement.dto.request.UpdateUserRequest;
import com.example.usermanagement.dto.response.ApiResponse;
import com.example.usermanagement.dto.response.PageResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all active users (paginated, with optional search)")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers(q, pageable)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication auth) {
        // Resolve the user from the security context username
        return ResponseEntity.ok(ApiResponse.ok(
            userService.listUsers(auth.getName(), PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    // Fallback: direct lookup by username
                    throw new com.example.usermanagement.exception.UserNotFoundException(
                        "Current user not found");
                })
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a user's profile (self or admin)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRequest request,
        Authentication auth
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            "User updated", userService.updateUser(id, request, auth.getName())));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Change password (self or admin)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
        @PathVariable UUID id,
        @Valid @RequestBody ChangePasswordRequest request,
        Authentication auth
    ) {
        userService.changePassword(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Password updated", null));
    }
}
