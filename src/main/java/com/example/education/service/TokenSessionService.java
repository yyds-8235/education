package com.example.education.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenSessionService {

    private final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public String createSession(String userId, long expiresInSeconds) {
        String token = UUID.randomUUID().toString().replace("-", "");
        SessionInfo sessionInfo = new SessionInfo(userId, Instant.now().plusSeconds(expiresInSeconds).toEpochMilli());
        sessions.put(token, sessionInfo);
        return token;
    }

    public Optional<String> getUserId(String token) {
        SessionInfo sessionInfo = sessions.get(token);
        if (sessionInfo == null) {
            return Optional.empty();
        }
        if (sessionInfo.expireAtMillis() <= Instant.now().toEpochMilli()) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(sessionInfo.userId());
    }

    public void removeSession(String token) {
        sessions.remove(token);
    }

    private record SessionInfo(String userId, long expireAtMillis) {
    }
}

