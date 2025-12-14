package com.cookmate.orchestrator.VoiceAssist.TTS;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TtsEventListener {

    private final AzureTtsService azureTtsService;            // text -> PCM(byte[])
    private final VoiceWebSocketSender voiceWebSocketSender;  // sessionId로 프론트에 전송

    @Async
    @EventListener
    public void onTtsRequest(TtsRequestEvent event) {
        log.info("[TTS EVT] received sessionId={}, text={}", event.sessionId(), event.text());
        try {
            byte[] pcm = azureTtsService.synthesizeToRawPcm(event.text());
            log.info("[TTS EVT] synthesized bytes={}", pcm.length);
            voiceWebSocketSender.sendPcm(event.sessionId(), pcm);
            log.info("[TTS EVT] sent");
        } catch (Exception e) {
            throw new RuntimeException("TTS failed: " + event.sessionId(), e);
        }
    }
}

