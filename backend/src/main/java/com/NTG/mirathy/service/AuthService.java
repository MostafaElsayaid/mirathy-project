package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.response.AuthResponse;
import com.NTG.mirathy.DTOs.request.LoginRequest;
import com.NTG.mirathy.DTOs.request.SignupRequest;
import com.NTG.mirathy.Entity.Enum.Role;
import com.NTG.mirathy.Entity.User;
import com.NTG.mirathy.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
            String token = jwtService.generateToken(userDetails);

            return AuthResponse.builder()
                    .accessToken(token)
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .isActive(user.isActive())
                    .createdAt(user.getCreatedAt())
                    .message("Login successful")
                    .success(true)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Invalid email or password: " + e.getMessage());
        }
    }

    public AuthResponse register(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);


        UserDetails userDetails = customUserDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .accessToken(token)
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .isActive(savedUser.isActive())
                .createdAt(savedUser.getCreatedAt())
                .message("Registration successful")
                .success(true)
                .build();
    }
}