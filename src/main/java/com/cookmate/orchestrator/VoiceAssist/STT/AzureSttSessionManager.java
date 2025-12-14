package com.cookmate.orchestrator.VoiceAssist.STT;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Azure STT 세션 관리하는 매니저
 * STT 세션 생성 + WebSocket에서 온 오디오 STT 세션으로 전달 + WebSocket 끊길 때 STT 세션 제거
 */
@Slf4j
@Service
public class AzureSttSessionManager {

    private final SpeechConfig speechConfig;     // STT 세션 만들 때 마다 필요한 공통 설정
    // sessions: <웹소켓 세션 ID, Azure STT 세션>
    private final Map<String, AzureContinuousSttSession> sessions = new ConcurrentHashMap<>();

    public AzureSttSessionManager(SpeechConfig speechConfig) {
        this.speechConfig = speechConfig;
    }

    /**
     * 새 WebSocket 연결이 생겼을 때 STT 세션을 만드는 함수
     */
    public void createSession(String sessionId, Consumer<String> onFinalText) throws Exception {

        // 이미 같은 sessionId가 있으면 다시 생성 X
        if (sessions.containsKey(sessionId)) {
            log.warn("[STT] session {} already exists", sessionId);
            return;
        }

        // 새 Azure continuous STT 세션 생성
        AzureContinuousSttSession session =
                new AzureContinuousSttSession(sessionId, speechConfig, onFinalText);   // Azure에서 "문장 끝" 이벤트가 오면 onFinalText 호출하도록 연결
        sessions.put(sessionId, session);
        log.info("[STT] session {} created", sessionId);
    }

    /**
     * WebSocket에서 받은 PCM chunk를 해당 세션으로 전달
     * */
    public void pushAudio(String sessionId, byte[] pcmChunk) {
        AzureContinuousSttSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("[STT] no session for id={}, dropping audio", sessionId);
            return;
        }

        session.pushAudio(pcmChunk);
    }

    /**
     * WebSocket 끊길 때 STT 세션 정리
     * */
    public void closeSession(String sessionId) {
        AzureContinuousSttSession session = sessions.remove(sessionId);
        if (session != null) {
            try {
                session.close();
                log.info("[STT] session {} closed", sessionId);
            } catch (IOException e) {
                log.error("[STT] error closing session {}: {}", sessionId, e.getMessage());
            }
        }
    }
}
