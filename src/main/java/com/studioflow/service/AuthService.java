package com.studioflow.service;

import com.studioflow.dto.auth.AuthResponse;
import com.studioflow.dto.auth.LoginRequest;
import com.studioflow.dto.auth.RegisterRequest;
import com.studioflow.dto.user.UserResponse;

import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser(String email);

    List<UserResponse> getActors();
}
