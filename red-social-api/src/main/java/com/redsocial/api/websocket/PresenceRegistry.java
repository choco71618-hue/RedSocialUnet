package com.redsocial.api.websocket;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceRegistry {

    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public void markConnected(String username) {
        lastSeen.put(username, Instant.now());
    }

    public void markDisconnected(String username) {
        lastSeen.put(username, Instant.now());
    }

    public Optional<Instant> lastSeen(String username) {
        return Optional.ofNullable(lastSeen.get(username));
    }
}