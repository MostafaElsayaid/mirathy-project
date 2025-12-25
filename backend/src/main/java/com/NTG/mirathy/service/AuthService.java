package com.NTG.mirathy.service;

import com.NTG.mirathy.DTOs.response.AuthResponse;
import com.NTG.mirathy.DTOs.request.LoginRequest;
import com.NTG.mirathy.DTOs.request.SignupRequest;
import com.NTG.mirathy.Entity.ActivationToken;
import com.NTG.mirathy.Entity.Enum.EmailTemplateName;
import com.NTG.mirathy.Entity.Enum.Role;
import com.NTG.mirathy.Entity.User;
import com.NTG.mirathy.Repository.ActivationTokenRepo;
import com.NTG.mirathy.Repository.UserRepository;
import com.NTG.mirathy.service.email.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final ActivationTokenRepo activationTokenRepo;
    private final EmailService emailService;

    @Value("${application.security.miling.frontend.activation-url}")
    private String activationUrl;

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

    public AuthResponse register(SignupRequest request) throws MessagingException {
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
        sendValidationEmail(savedUser);

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

    private void  sendValidationEmail(User savedUser) throws MessagingException {
        var newToken = generateAndSaveActivationToken(savedUser);
        emailService.sendEmail(
                savedUser.getEmail(),
                savedUser.getFullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Activate Your Account"
        );
    }

    private String generateAndSaveActivationToken(User savedUser) {
        // generate a token
        String generatedToken = generateActinationToken(4);
        var token = ActivationToken.builder()
                .token(generatedToken)
                .createdAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(15))
                .user(savedUser)
                .build();
        activationTokenRepo.save(token);
        return generatedToken;
    }

    private String generateActinationToken(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            token.append(characters.charAt(index));
        }
        return token.toString();
    }

}