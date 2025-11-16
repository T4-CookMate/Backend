package com.cookmate.orchestrator.VoiceAssist.STT;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * 한 WebSocket 연결에 대응하는, Azure continuous STT 세션 한 덩어리
 */
@Slf4j
public class AzureContinuousSttSession implements Closeable {

    private final String sessionId;                // 이 세션이 어떤 WebSocket 연결에서 온 건지 식별용 ID (로그 찍을 때도 같이 사용)
    private final SpeechRecognizer recognizer;     // Azure Speech SDK의 연속 음성 인식기 (continuous STT의 본체)
    private final PushAudioInputStream pushStream; // PCM chunk를 write()하며 Azure recognizer가 실시간으로 읽어가는 오디오 스트림

    public AzureContinuousSttSession(
            String sessionId,
            SpeechConfig speechConfig,
            Consumer<String> onFinalText
    ) throws Exception {
        this.sessionId = sessionId;                // 이 STT 세션이 어떤 WebSocket 연결과 연결되어 있는지 내부에 기록

        // PCM 포맷 (프론트와 반드시 맞춰야 함!)
        AudioStreamFormat format =
                AudioStreamFormat.getWaveFormatPCM(16000, (short) 16, (short) 1);

        this.pushStream = AudioInputStream.createPushStream(format);        // 오디오를 write()해서 넣는 스트림
        AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);  // recognizr가 오디오를 어디서 읽을지 설정하는 객체

        this.recognizer = new SpeechRecognizer(speechConfig, audioConfig);  // 오디오 스트림을 계속 듣는 Azure 인식기 생성

        // 중간 결과 (원하면 로그만 찍고 무시해도 됨): 말하는 도중의 부분 텍스트 받는 콜백
        recognizer.recognizing.addEventListener((o, e) -> {
            String partial = e.getResult().getText();
            if (partial != null && !partial.isEmpty()) {
                log.debug("[STT-{}] partial: {}", sessionId, partial);
            }
        });

        // 최종 결과: Azure가 "한 문장이 끝났다"라고 인식했을 때 호출
        recognizer.recognized.addEventListener((o, e) -> {

            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String text = e.getResult().getText();
                log.info("[STT-{}] final: {}", sessionId, text);
                // 👉 최종 텍스트를 상위 레이어로 전달 (WebSocket 통해 클라로, 또는 NLU로)
                onFinalText.accept(text);
            } else {
                log.warn("[STT-{}] no speech recognized. reason={}",
                        sessionId, e.getResult().getReason());
            }
        });

        // 상태/에러 로그용 이벤트들
        recognizer.canceled.addEventListener((o, e) -> {
            log.warn("[STT-{}] canceled: {} - {}", sessionId, e.getErrorCode(), e.getErrorDetails());
        });
        recognizer.sessionStarted.addEventListener((o, e) ->
                log.info("[STT-{}] session started", sessionId));
        recognizer.sessionStopped.addEventListener((o, e) ->
                log.info("[STT-{}] session stopped", sessionId));

        // continuous 모드 시작
        recognizer.startContinuousRecognitionAsync().get();
    }

    /**
     * WebSocket에서 받은 PCM chunk를 Azure로 밀어넣기
     * */
    public void pushAudio(byte[] pcmChunk) {
        if (pcmChunk == null || pcmChunk.length == 0) return;
        pushStream.write(pcmChunk);
    }

    /**
     * 세션 정리: WebSocket이 끊기거나, 세션을 더이상 쓰지 않을 때 호출
     */
    @Override
    public void close() throws IOException {
        try {
            recognizer.stopContinuousRecognitionAsync().get();    // continuous 인식 중지
        } catch (Exception e) {
            log.warn("[STT-{}] error stopping recognizer: {}", sessionId, e.getMessage());
        }
        pushStream.close();  // 오디오 스트림 닫기
        recognizer.close();  // recognizer 리소스 해제
    }
}
