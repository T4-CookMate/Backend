package com.cookmate.orchestrator.VoiceAssist.NLU;

public enum IntentType {
    START,                // 시작
    INGREDIENT_STATE,     // 재료 상태
    NEXT_STEP,            // 다음 단계 이동
    NEXT_STEP_QUESTION,   // 다음 단계 질문
    DANGER_CHECK,         // 위험 요소
    UNKNOWN
}