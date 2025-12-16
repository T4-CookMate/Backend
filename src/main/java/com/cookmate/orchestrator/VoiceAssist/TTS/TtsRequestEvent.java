package com.cookmate.orchestrator.VoiceAssist.TTS;

public record TtsRequestEvent(
        String sessionId,
        String text,
        boolean closeAfterSend
) {}
