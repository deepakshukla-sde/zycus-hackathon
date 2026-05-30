package com.zycus.hackathon.service;

import com.zycus.hackathon.dto.AuthResponse;
import com.zycus.hackathon.dto.LoginRequest;
import com.zycus.hackathon.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
