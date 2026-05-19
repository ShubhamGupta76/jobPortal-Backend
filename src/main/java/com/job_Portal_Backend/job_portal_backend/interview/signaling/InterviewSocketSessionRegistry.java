package com.job_Portal_Backend.job_portal_backend.interview.signaling;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InterviewSocketSessionRegistry {
    private final Map<String, Map<Long, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public void register(String roomToken, Long userId, WebSocketSession session) {
        rooms.computeIfAbsent(roomToken, key -> new ConcurrentHashMap<>()).put(userId, session);
    }

    public void unregister(String roomToken, Long userId) {
        Map<Long, WebSocketSession> participants = rooms.get(roomToken);
        if (participants == null) {
            return;
        }
        participants.remove(userId);
        if (participants.isEmpty()) {
            rooms.remove(roomToken);
        }
    }

    public Map<Long, WebSocketSession> participants(String roomToken) {
        return rooms.getOrDefault(roomToken, Collections.emptyMap());
    }

    public Set<Long> participantIds(String roomToken) {
        return participants(roomToken).keySet();
    }
}
