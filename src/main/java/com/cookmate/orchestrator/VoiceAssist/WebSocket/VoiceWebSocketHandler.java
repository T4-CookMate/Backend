package com.cookmate.orchestrator.VoiceAssist.WebSocket;

import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.Exception.GeneralException;
import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;
import com.cookmate.orchestrator.User.Entity.User;
import com.cookmate.orchestrator.Recipe.Repository.RecipeRepository;
import com.cookmate.orchestrator.Recipe.Service.RecipeProgressService;
import com.cookmate.orchestrator.User.Repository.UserRepository;
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

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final RecipeRepository recipeRepository;

    /**
     * 새로운 WebSocket 연결 생성 시 호출 -> Azure continuous STT 세션 생성
     * Azure STT가 음성 인식 후 문장 하나가 끝나면 그 텍스트를 해당 WebSocket으로 다시 보내도록 콜백 설정
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();

        Long userId = (Long) session.getAttributes().get("userId");
        User user = (User) session.getAttributes().get("user");
        Long recipeId = (Long) session.getAttributes().get("recipeId");

        // --- 인증/파라미터 검증 ---
        if (userId == null || user == null) {
            log.error("[WS] 인증 정보가 없습니다. userId={}, user={}", userId, user);
            sttSessionManager.closeSession(sessionId);
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }
        if (recipeId == null) {
            log.error("[WS] recipeId 쿼리 파라미터가 없습니다.");
            sttSessionManager.closeSession(sessionId);
            throw new GeneralException(ErrorStatus.VALIDATION_ERROR, "recipeId 쿼리 파라미터가 없습니다.");
        }
        if (recipeRepository.findById(recipeId).isEmpty()) {
            log.error("Recipe with id {} not found", recipeId);
            sttSessionManager.closeSession(sessionId);
            throw new GeneralException(ErrorStatus.RECIPE_NOT_FOUND);
        }

        // --- 레시피 진행 세션 시작 ---
        // 사용자가 해당 레시피를 이미 진행 중인지 확인 -> 진행중이면 종료가 잘 안된 것이므로 종료 후 재시도
        Optional<RecipeProgress> progressOpt = progressService.startSession(userId, recipeId, sessionId);
        if (progressOpt.isEmpty()) {
            progressService.cleanup(sessionId);
            progressOpt = progressService.startSession(userId, recipeId, sessionId);
        }

        // 한 차례 정리 후에도 Progress가 종료되지 않은 경우
        if (progressOpt.isEmpty()) {
            throw new GeneralException(ErrorStatus.SESSION_ALREADY_START);
        }

        // STT 세션 생성: STT 결과가 나오면 해당 WebSocket으로 바로 전송
        sttSessionManager.createSession(sessionId, finalText -> {
            try {
                // 호출 키워드 체크
                if (!finalText.trim().startsWith("쿡짝꿍") && !finalText.trim().startsWith("국자꾼")) {
                    log.info("[STT] 호출어 없음 → 무시됨: {}", finalText);
                    return; // 아래 로직 모두 스킵
                }

                // 호출어 "쿡짝꿍" 제거
                String cleanedText = finalText.replaceFirst("^쿡짝꿍", "").trim();

                // STT 결과 -> NLU 분석 -> 질문 의도 파악
                IntentResult intent = nluService.analyze(cleanedText);

                // intent에 맞게 답변 텍스트 만들기
                String answerText = dialogueService.handleIntent(sessionId, intent);

                // TTS: 답변 텍스트 → 오디오 바이트
                byte[] audioBytes = azureTtsService.synthesizeToRawPcm(answerText);

                session.sendMessage(new BinaryMessage(audioBytes));
            } catch (Exception e) {
                log.error("[WS] failed to send STT result to client: {}", e.getMessage());
                try {
                    session.sendMessage(new TextMessage("서버에서 음성 응답 생성 중 오류가 발생했어요."));
                } catch (Exception ignore) {
                    throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR, "STT 중 발생한 오류 메세지 전달에 실패했어요.");
                }
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
        safeCleanup(session);
        log.info("[WS] voice socket closed: {}", session.getId());
    }

    /**
     * 네트워크 에러와 같은 오류 발생 시 호출
     * 에러 로그, 세션 강제 종료, 내부 리소스 정리 등 수행
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        log.error("[WS] transport error on {}: {}", session.getId(), exception.getMessage());

        safeCleanup(session); // 중복 호출 안전하게 처리됨
        session.close(CloseStatus.SERVER_ERROR);
    }

    private void safeCleanup(WebSocketSession session) {
        Boolean cleaned = (Boolean) session.getAttributes().get("CLEANED_UP");
        if (cleaned != null && cleaned) {
            return;
        }

        String sessionId = session.getId();
        sttSessionManager.closeSession(sessionId);
        progressService.cleanup(sessionId);

        session.getAttributes().put("CLEANED_UP", true);
    }
}
