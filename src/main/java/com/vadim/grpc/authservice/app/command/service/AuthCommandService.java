package com.vadim.grpc.authservice.app.command.service;


import com.vadim.grpc.authservice.domain.model.PasswordPolicy;
import com.vadim.grpc.authservice.domain.model.UserEntity;
import com.vadim.grpc.authservice.domain.repository.UserRepository;
import com.vadim.grpc.authservice.infra.email.EmailService;
import com.vadim.grpc.authservice.infra.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthCommandService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;


    @Transactional
    public String register(String email, String password) {
        if (!PasswordPolicy.validate(password)) {
            throw new IllegalArgumentException("Password does not meet policy");
        }
        userRepository.findByEmail(email.toLowerCase()).ifPresent(u -> {
            throw new IllegalArgumentException("Email already in use");
        });
        String hashed = passwordEncoder.encode(password);
        UserEntity user = UserEntity.register(email, hashed, Instant.now());
        user.confirm();
        userRepository.save(user);
        return user.getId();
    }

    @Transactional
    public AuthResult authenticate(String email, String password) {
        Optional<UserEntity> ou = userRepository.findByEmail(email.toLowerCase());
        if (ou.isEmpty()) throw new IllegalArgumentException("Invalid credentials");
        UserEntity user = ou.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwtService.generateRefreshToken();
        String refreshHash = passwordEncoder.encode(refresh);
        user.setRefreshToken(refreshHash, Instant.now().plusSeconds(60*60*24*30));
        userRepository.save(user);
        return new AuthResult(access, refresh, user.getId());
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<UserEntity> ou = userRepository.findByEmail(email.toLowerCase());
        if (ou.isEmpty()) {
            return;
        }
        UserEntity user = ou.get();
        Instant now = Instant.now();
        if (user.getLastResetRequestedAt() != null && now.isBefore(user.getLastResetRequestedAt().plusSeconds(60*5))) {
            throw new IllegalStateException("Too many reset requests. Try later.");
        }
        String token = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(token);
        user.setResetToken(tokenHash, now.plusSeconds(60*60), now);
        emailService.sendPasswordReset(email, token);
    }

    @Transactional
    public AuthResult resetPassword(String email, String token, String newPassword) {
        UserEntity user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token or email"));
        Instant now = Instant.now();
        if (!user.verifyResetToken(token, now, passwordEncoder)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        if (!PasswordPolicy.validate(newPassword)) {
            throw new IllegalArgumentException("Password policy");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwtService.generateRefreshToken();
        user.setRefreshToken(passwordEncoder.encode(refresh), now.plusSeconds(60*60*24*30));
        return new AuthResult(access, refresh, user.getId());
    }

    public record AuthResult(String accessToken, String refreshToken, String userId) {
    }

}