package com.cookmate.orchestrator.VoiceAssist.TTS;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VoiceWebSocketSender {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    public void sendPcm(String sessionId, byte[] pcm) throws IOException {
        WebSocketSession s = sessions.get(sessionId);
        if (s == null || !s.isOpen()) return;

        s.sendMessage(new BinaryMessage(pcm));
    }
}

