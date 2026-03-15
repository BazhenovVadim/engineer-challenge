package com.vadim.grpc.authservice;

import com.vadim.grpc.authservice.app.command.service.AuthCommandService;
import com.vadim.grpc.authservice.domain.model.UserEntity;
import com.vadim.grpc.authservice.domain.repository.UserRepository;
import com.vadim.grpc.authservice.infra.email.EmailService;
import com.vadim.grpc.authservice.infra.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private AuthCommandService authCommandService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private JwtService jwtService;

    private final String testEmail = "test@example.com";
    private final String testPassword = "Password123";

    @BeforeEach
    void setUp() {
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
    }

    @Test
    void shouldRegisterUser() {
        String userId = authCommandService.register(testEmail, testPassword);
        assertNotNull(userId);

        Optional<UserEntity> saved = userRepository.findById(userId);
        assertTrue(saved.isPresent());
        assertEquals(testEmail.toLowerCase(), saved.get().getEmail());
        assertTrue(passwordEncoder.matches(testPassword, saved.get().getPasswordHash()));
    }

    @Test
    void shouldNotRegisterDuplicateEmail() {
        authCommandService.register(testEmail, testPassword);
        assertThrows(IllegalArgumentException.class, () -> {
            authCommandService.register(testEmail, testPassword);
        });
    }

    @Test
    void shouldAuthenticateUser() {
        String userId = authCommandService.register(testEmail, testPassword);
        AuthCommandService.AuthResult result = authCommandService.authenticate(testEmail, testPassword);
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertNotNull(result.accessToken());
        assertNotNull(result.refreshToken());

        UserEntity user = userRepository.findById(userId).get();
        assertNotNull(user.getRefreshTokenHash());
        assertNotNull(user.getRefreshTokenExpiry());
    }

    @Test
    void shouldRejectInvalidCredentials() {
        authCommandService.register(testEmail, testPassword);
        assertThrows(IllegalArgumentException.class, () -> {
            authCommandService.authenticate(testEmail, "WrongPass123");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            authCommandService.authenticate("wrong@email.com", testPassword);
        });
    }

    @Test
    void shouldRequestPasswordReset() {
        authCommandService.register(testEmail, testPassword);
        authCommandService.requestPasswordReset(testEmail);
        verify(emailService, times(1)).sendPasswordReset(eq(testEmail), anyString());
        UserEntity user = userRepository.findByEmail(testEmail.toLowerCase()).get();
        assertNotNull(user.getResetTokenHash());
        assertNotNull(user.getResetTokenExpiry());
        assertNotNull(user.getLastResetRequestedAt());
    }

    @Test
    void shouldResetPassword() {
        authCommandService.register(testEmail, testPassword);
        String[] capturedToken = new String[1];
        doAnswer(invocation -> {
            capturedToken[0] = invocation.getArgument(1);
            return null;
        }).when(emailService).sendPasswordReset(anyString(), anyString());

        authCommandService.requestPasswordReset(testEmail);
        String resetToken = capturedToken[0];

        String newPassword = "NewPass123";

        AuthCommandService.AuthResult result = authCommandService.resetPassword(
                testEmail, resetToken, newPassword);

        assertNotNull(result);

        UserEntity user = userRepository.findByEmail(testEmail.toLowerCase()).get();
        assertTrue(passwordEncoder.matches(newPassword, user.getPasswordHash()));
        assertNull(user.getResetTokenHash());
        assertNull(user.getResetTokenExpiry());
        AuthCommandService.AuthResult loginResult =
                authCommandService.authenticate(testEmail, newPassword);
        assertNotNull(loginResult);
    }

    @Test
    void shouldNotResetPasswordWithUsedToken() {
        authCommandService.register(testEmail, testPassword);

        String[] capturedToken = new String[1];
        doAnswer(invocation -> {
            capturedToken[0] = invocation.getArgument(1);
            return null;
        }).when(emailService).sendPasswordReset(anyString(), anyString());
        authCommandService.requestPasswordReset(testEmail);
        String resetToken = capturedToken[0];
        authCommandService.resetPassword(testEmail, resetToken, "NewPass123");
        assertThrows(IllegalArgumentException.class, () -> {
            authCommandService.resetPassword(testEmail, resetToken, "AnotherPass123");
        });
    }

    @Test
    void shouldEnforceRateLimitOnResetRequests() {
        authCommandService.register(testEmail, testPassword);
        authCommandService.requestPasswordReset(testEmail);
        assertThrows(IllegalStateException.class, () -> {
            authCommandService.requestPasswordReset(testEmail);
        });
    }
}