package com.vadim.grpc.authservice.utils;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final ConcurrentHashMap<String, Instant> lastMap = new ConcurrentHashMap<>();
    private final long cooldownSeconds;

    public RateLimiter(long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        Instant last = lastMap.get(key);
        if (last == null || now.isAfter(last.plusSeconds(cooldownSeconds))) {
            lastMap.put(key, now);
            return true;
        }
        return false;
    }
}