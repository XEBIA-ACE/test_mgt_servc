package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.ChangePasswordRequest;
import com.example.usermanagement.dto.request.RegisterRequest;
import com.example.usermanagement.dto.request.UpdateUserRequest;
import com.example.usermanagement.dto.response.PageResponse;
import com.example.usermanagement.dto.response.UserResponse;
import com.example.usermanagement.model.entity.Role;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse register(RegisterRequest request);

    UserResponse getById(UUID id);

    UserResponse getByUsername(String username);

    PageResponse<UserResponse> listUsers(Pageable pageable);

    PageResponse<UserResponse> searchUsers(String query, Pageable pageable);

    UserResponse update(UUID id, UpdateUserRequest request);

    void changePassword(UUID id, ChangePasswordRequest request);

    void assignRole(UUID id, Role role);

    void removeRole(UUID id, Role role);

    void disableUser(UUID id);

    void enableUser(UUID id);

    void deleteUser(UUID id);
}
