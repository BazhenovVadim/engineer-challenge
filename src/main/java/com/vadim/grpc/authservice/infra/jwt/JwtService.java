package com.vadim.grpc.authservice.infra.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.UUID;

@Service
public class JwtService {
    private final Key key;

    public JwtService(org.springframework.core.env.Environment env) {
        String secret = env.getProperty("JWT_SECRET", "defaultsecretforsample");
        byte[] bytes = Base64.getEncoder().encode(secret.getBytes());
        this.key = new SecretKeySpec(bytes, SignatureAlgorithm.HS256.getJcaName());
    }

    public String generateAccessToken(String userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(60*15)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}