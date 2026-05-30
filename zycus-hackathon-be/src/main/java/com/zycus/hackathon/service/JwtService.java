package com.zycus.hackathon.service;

public interface JwtService {
    String generateToken(Long userId);
    Long extractUserId(String token);
    boolean isTokenValid(String token);
}
