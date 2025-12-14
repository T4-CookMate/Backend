package com.cookmate.orchestrator.VoiceAssist.NLU;

public enum IntentType {
    START,             // 시작
    INGREDIENT_STATE,  // 재료 상태
    NEXT_STEP,         // 요리 단계
    DANGER_CHECK,      // 위험 요소
    UNKNOWN
}