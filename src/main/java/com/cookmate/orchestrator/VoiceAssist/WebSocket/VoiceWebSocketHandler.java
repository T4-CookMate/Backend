package com.cookmate.orchestrator.VoiceAssist.WebSocket;

import com.cookmate.orchestrator.Recipe.Service.RecipeProgressService;
import com.cookmate.orchestrator.VoiceAssist.STT.AzureSttSessionManager;
import com.cookmate.orchestrator.VoiceAssist.NLU.DialogueService;
import com.cookmate.orchestrator.VoiceAssist.NLU.IntentResult;
import com.cookmate.orchestrator.VoiceAssist.NLU.NLUService;
import com.cookmate.orchestrator.VoiceAssist.TTS.AzureTtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 웹에서 전달되는 PCM 오디오 데이터를 받아 Azure STT로 스트리밍하고,
 * 결과를 다시 클라이언트로 보내주는 WebSocket 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler extends BinaryWebSocketHandler {

    private final AzureSttSessionManager sttSessionManager;
    private final RecipeProgressService progressService;
    private final NLUService nluService;
    private final DialogueService dialogueService;
    private final AzureTtsService azureTtsService;

    /**
     * 새로운 WebSocket 연결 생성 시 호출 -> Azure continuous STT 세션 생성
     * Azure STT가 음성 인식 후 문장 하나가 끝나면 그 텍스트를 해당 WebSocket으로 다시 보내도록 콜백 설정
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // TODO: 로그인 구현 후 userId는 인증 로직에서 빼내옴 (지금은 쿼리파라미터에서 userId, recipeId 꺼내온다고 가정)
        Map<String, String> params = parseQueryParams(session.getUri().getQuery());

        Long userId = Long.valueOf(params.get("userId"));
        Long recipeId = Long.valueOf(params.get("recipeId"));

        String sessionId = session.getId();

        progressService.startSession(userId, recipeId, sessionId);

        // STT 세션 생성: STT 결과가 나오면 해당 WebSocket으로 바로 전송
        sttSessionManager.createSession(sessionId, finalText -> {
            try {
                // STT 결과 -> NLU 분석 -> 질문 의도 파악
                IntentResult intent = nluService.analyze(finalText);

                // intent에 맞게 답변 텍스트 만들기
                String answerText = dialogueService.handleIntent(sessionId, intent);

                // TTS: 답변 텍스트 → 오디오 바이트
                byte[] audioBytes = azureTtsService.synthesizeToRawPcm(answerText);

                session.sendMessage(new BinaryMessage(audioBytes));
            } catch (Exception e) {
                log.error("[WS] failed to send STT result to client: {}", e.getMessage());
                try {
                    session.sendMessage(new TextMessage("서버에서 음성 응답 생성 중 오류가 발생했어요."));
                } catch (Exception ignore) {}
            }
        });
        log.info("[WS] voice socket connected: {}", sessionId);
    }

    /**
     * 프론트에서 보내온 raw PCM 오디오 chunk 받는 함수 (프론트가 일정 주기로 PCM 전송)
     * 해당 chunk를 byte로 변환 후 Azure continuous STT 세션에 전달
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();
        ByteBuffer buffer = message.getPayload();
        byte[] pcm = new byte[buffer.remaining()];
        buffer.get(pcm);

        // PCM chunk를 STT 세션에 전달
        sttSessionManager.pushAudio(sessionId, pcm);
    }

    /**
     * WebSocket 연결 종료 시 호출 -> 해당 WebSocket에 연결된 Azure STT 세션 종료
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sttSessionManager.closeSession(sessionId);
        log.info("[WS] voice socket closed: {}", sessionId);
    }

    /**
     * 쿼리 파싱
     */
    private Map<String, String> parseQueryParams(String query) {
        if (query == null || query.isBlank()) return Map.of();

        return Arrays.stream(query.split("&"))
                .map(p -> p.split("=", 2))
                .filter(kv -> kv.length == 2)
                .collect(Collectors.toMap(
                        kv -> URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        kv -> URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                ));
    }
}
