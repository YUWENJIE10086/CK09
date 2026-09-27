package com.barn.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    @Test
    void locksAfterFiveFailuresAndClearsOnSuccess() {
        LoginAttemptService service = new LoginAttemptService();

        assertFalse(service.isLocked(" Admin "));

        for (int i = 0; i < 4; i++) {
            service.recordFailure(" Admin ");
            assertFalse(service.isLocked("admin"));
        }

        service.recordFailure(" Admin ");
        assertTrue(service.isLocked("admin"));

        service.recordSuccess("ADMIN");
        assertFalse(service.isLocked("admin"));
    }

    @Test
    void normalizesUsernameCaseAndWhitespace() {
        LoginAttemptService service = new LoginAttemptService();

        service.recordFailure("  TestUser  ");
        assertFalse(service.isLocked("testuser"));

        for (int i = 0; i < 4; i++) {
            service.recordFailure("TESTUSER");
        }
        assertTrue(service.isLocked(" testuser "));
    }
}
