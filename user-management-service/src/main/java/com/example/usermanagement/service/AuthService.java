package com.example.usermanagement.service;

import com.example.usermanagement.model.dto.request.LoginRequest;
import com.example.usermanagement.model.dto.request.RefreshTokenRequest;
import com.example.usermanagement.model.dto.request.RegisterRequest;
import com.example.usermanagement.model.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);
}
