package com.example.usermanagement.service;

import com.example.usermanagement.dto.request.LoginRequest;
import com.example.usermanagement.dto.request.RefreshTokenRequest;
import com.example.usermanagement.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String refreshToken);
}
