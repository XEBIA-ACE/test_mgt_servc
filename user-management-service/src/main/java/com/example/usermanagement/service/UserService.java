package com.example.usermanagement.service;

import com.example.usermanagement.model.dto.request.ChangePasswordRequest;
import com.example.usermanagement.model.dto.request.UpdateUserRequest;
import com.example.usermanagement.model.dto.response.PagedResponse;
import com.example.usermanagement.model.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getUserById(UUID id);

    UserResponse getUserByEmail(String email);

    PagedResponse<UserResponse> getAllUsers(int page, int size, String search);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void deleteUser(UUID id);

    void changePassword(UUID id, ChangePasswordRequest request);

    UserResponse updateUserRole(UUID id, String role);
}
