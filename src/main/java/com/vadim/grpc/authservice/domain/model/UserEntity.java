package com.vadim.grpc.authservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;


@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    private boolean confirmed;
    private Instant createdAt;

    // reset token fields (single-use, time-limited)
    private String resetTokenHash;
    private Instant resetTokenExpiry;
    private Instant lastResetRequestedAt;

    // refresh token (hashed)
    private String refreshTokenHash;
    private Instant refreshTokenExpiry;

    private UserEntity(String id, String email, String passwordHash, Instant now) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.confirmed = false;
        this.createdAt = now;
    }

    public static UserEntity register(String email, String passwordHash, Instant now) {
        // invariants: passwordHash assumed hashed; email non-null validated outside
        return new UserEntity(null, email.toLowerCase(), passwordHash, now);
    }

    public boolean checkPassword(String rawOrHash, java.util.function.BiPredicate<String, String> verifier) {
        return verifier.test(rawOrHash, passwordHash);
    }

    public void setPassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        // invalidate reset token
        this.resetTokenHash = null;
        this.resetTokenExpiry = null;
    }

    public void setResetToken(String tokenHash, Instant expiry, Instant requestAt) {
        this.resetTokenHash = tokenHash;
        this.resetTokenExpiry = expiry;
        this.lastResetRequestedAt = requestAt;
    }

    public boolean verifyResetToken(String rawToken, Instant now, PasswordEncoder encoder) {
        if (resetTokenHash == null || resetTokenExpiry == null) return false;
        if (now.isAfter(resetTokenExpiry)) return false;

        // Сравниваем raw token с хешем
        boolean ok = encoder.matches(rawToken, resetTokenHash);

        if (ok) {
            // consume token (single-use)
            this.resetTokenHash = null;
            this.resetTokenExpiry = null;
        }
        return ok;
    }

    public void setRefreshToken(String refreshTokenHash, Instant expiry) {
        this.refreshTokenHash = refreshTokenHash;
        this.refreshTokenExpiry = expiry;
    }

    public boolean verifyRefreshToken(String tokenHash, Instant now) {
        if (refreshTokenHash == null || refreshTokenExpiry == null) return false;
        if (now.isAfter(refreshTokenExpiry)) return false;
        return refreshTokenHash.equals(tokenHash);
    }

    public void confirm() {
        this.confirmed = true;
    }
}