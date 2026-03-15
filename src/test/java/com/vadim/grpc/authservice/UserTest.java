package com.vadim.grpc.authservice;

import com.vadim.grpc.authservice.domain.model.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void registerUser() {

        UserEntity user = UserEntity.register(
                "test@test.com",
                "hashed_password",
                Instant.now()
        );

        assertNotNull(user.getId());
        assertEquals("test@test.com", user.getEmail());
    }

}