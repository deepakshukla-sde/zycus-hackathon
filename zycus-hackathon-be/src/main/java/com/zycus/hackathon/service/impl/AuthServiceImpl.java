package com.zycus.hackathon.service.impl;

import com.zycus.hackathon.dto.AuthResponse;
import com.zycus.hackathon.dto.LoginRequest;
import com.zycus.hackathon.dto.RegisterRequest;
import com.zycus.hackathon.entity.Profile;
import com.zycus.hackathon.repository.ProfileRepository;
import com.zycus.hackathon.service.AuthService;
import com.zycus.hackathon.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (profileRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        Profile profile = Profile.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        profile = profileRepository.save(profile);
        String token = jwtService.generateToken(profile.getId());
        return new AuthResponse(token, profile.getId(), profile.getName(), profile.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Profile profile = profileRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), profile.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        String token = jwtService.generateToken(profile.getId());
        return new AuthResponse(token, profile.getId(), profile.getName(), profile.getEmail());
    }
}
