package com.job_Portal_Backend.job_portal_backend.interview.config;

import com.job_Portal_Backend.job_portal_backend.interview.websocket.InterviewSignalingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class InterviewWebSocketConfig implements WebSocketConfigurer {

    private final InterviewSignalingWebSocketHandler interviewSignalingWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interviewSignalingWebSocketHandler, "/interview-signal")
                .setAllowedOriginPatterns("*");
    }
}
