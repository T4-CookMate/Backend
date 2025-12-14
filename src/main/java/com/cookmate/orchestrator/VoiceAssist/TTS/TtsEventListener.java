package com.cookmate.orchestrator.VoiceAssist.TTS;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TtsEventListener {

    private final AzureTtsService azureTtsService;            // text -> PCM(byte[])
    private final VoiceWebSocketSender voiceWebSocketSender;  // sessionId로 프론트에 전송

    @Async
    @EventListener
    public void onTtsRequest(TtsRequestEvent event) {
        try {
            byte[] pcm = azureTtsService.synthesizeToRawPcm(event.text());
            voiceWebSocketSender.sendPcm(event.sessionId(), pcm);
        } catch (Exception e) {
            throw new RuntimeException("TTS failed: " + event.sessionId(), e);
        }
    }
}

