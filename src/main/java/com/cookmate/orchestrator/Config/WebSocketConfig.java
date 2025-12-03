package com.cookmate.orchestrator.Config;

import com.cookmate.orchestrator.Common.Security.Jwt.JwtHandshakeInterceptor;
import com.cookmate.orchestrator.VoiceAssist.WebSocket.VoiceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/**
 * 브라우저 (WebSocket 클라이언트)가 접속할 수 있는 WebSocket 주소(/ws/voice)를 스프링 서버에 등록해주는 설정파일
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoiceWebSocketHandler voiceWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        /** WebSocket 접속 경로를 서버에 등록 후 핸드쉐이크 요청 들어오면 voiceWebSocketHandler가 처리 */
        registry.addHandler(voiceWebSocketHandler, "/ws/voice")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins("*");                        // 개발단계에서는 모든 도메인 허용
                //.setAllowedOrigins("https://cookmate.com");   // TODO: 프론트 도메인 맞춰 수정
    }
}