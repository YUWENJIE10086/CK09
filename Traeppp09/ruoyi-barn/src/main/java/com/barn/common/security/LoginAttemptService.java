package com.barn.common.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final class State {
        int failures;
        Instant firstFailure;
        Instant lockedUntil;
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public boolean isLocked(String username) {
        String key = normalize(username);
        State state = states.get(key);
        if (state == null) return false;
        Instant now = Instant.now();
        if (state.lockedUntil != null && now.isBefore(state.lockedUntil)) return true;
        if (state.lockedUntil != null) states.remove(key);
        return false;
    }

    public void recordFailure(String username) {
        String key = normalize(username);
        states.compute(key, (k, state) -> {
            Instant now = Instant.now();
            if (state == null || state.firstFailure == null || now.isAfter(state.firstFailure.plus(LOCK_DURATION))) {
                state = new State();
                state.firstFailure = now;
            }
            state.failures++;
            if (state.failures >= MAX_FAILURES) state.lockedUntil = now.plus(LOCK_DURATION);
            return state;
        });
    }

    public void recordSuccess(String username) {
        states.remove(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
