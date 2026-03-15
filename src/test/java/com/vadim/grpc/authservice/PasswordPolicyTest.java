package com.vadim.grpc.authservice;

import com.vadim.grpc.authservice.domain.model.PasswordPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    @Test
    void validPasswordShouldPass() {
        assertTrue(PasswordPolicy.validate("StrongPass123"));
    }

    @Test
    void shortPasswordShouldFail() {
        assertFalse(PasswordPolicy.validate("123"));
    }

    @Test
    void nullPasswordShouldFail() {
        assertFalse(PasswordPolicy.validate(null));
    }
}