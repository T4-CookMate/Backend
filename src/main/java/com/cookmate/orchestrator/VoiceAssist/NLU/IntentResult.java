package com.cookmate.orchestrator.VoiceAssist.NLU;

import java.util.Map;

public record IntentResult(
        IntentType type,
        Map<String, String> slots,   // 부가 정보 (e.g., direction=right, ingredient=고기)
        String originalText          // 원문 전체
) {}