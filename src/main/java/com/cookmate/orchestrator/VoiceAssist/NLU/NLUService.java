package com.cookmate.orchestrator.VoiceAssist.NLU;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class NLUService {

    public IntentResult analyze(String text) {

        // text가 null이면 UNKNOWN 타입 반환
        if (text == null || text.isBlank()) {
            return new IntentResult(IntentType.UNKNOWN, Map.of(), text);
        }

        String normalized = normalize(text);          // 공백 제거 및 소문자
        Map<String, String> slots = new HashMap<>();  // 슬롯(부가 정보)들을 담을 맵

        /** 1. 방향 정보 추출 (위험 요소 질문에 사용) */
        if (normalized.contains("왼쪽") || normalized.contains("좌")) {
            slots.put("direction", "left");
        } else if (normalized.contains("오른쪽") || normalized.contains("우")) {
            slots.put("direction", "right");
        } else if (normalized.contains("앞") || normalized.contains("전방")) {
            slots.put("direction", "front");
        } else if (normalized.contains("뒤") || normalized.contains("후방") || normalized.contains("뒷")) {
            slots.put("direction", "back");
        }

        /** 2. 재료 후보 단순 추출 (고기/계란/양파 정도만 예시) */
        if (normalized.contains("고기")) {
            slots.put("ingredient", "고기");
        } else if (normalized.contains("계란") || normalized.contains("달걀")) {
            slots.put("ingredient", "계란");
        } else if (normalized.contains("양파")) {
            slots.put("ingredient", "양파");
        } else if (normalized.contains("밥") || normalized.contains("햇반")){
            slots.put("ingredient", "밥");
        } else if (normalized.contains("간장")) {
            slots.put("ingredient", "간장");
        } else if (normalized.contains("참기름")) {
            slots.put("ingredient", "참기름");
        }

        /** 3. Intent 분류 */
        if (isNextStepQuestion(normalized)) {
            return new IntentResult(IntentType.NEXT_STEP, slots, text);         // 요리 다음 단계 질문
        }

        if (isIngredientStateQuestion(normalized)) {
            return new IntentResult(IntentType.INGREDIENT_STATE, slots, text);  // 재료 상태 질문
        }

        if (isDangerCheckQuestion(normalized)) {
            return new IntentResult(IntentType.DANGER_CHECK, slots, text);      // 위험 요소 질문
        }

        return new IntentResult(IntentType.UNKNOWN, slots, text);               // 그 밖에는 UNKNOWN
    }

    /**
     * 공백 제거 + 소문자
     */
    private String normalize(String text) {
        return text
                .replace(" ", "")
                .toLowerCase(Locale.KOREAN);
    }

    /**
     * 다음 단계 관련 질문
     */
    private boolean isNextStepQuestion(String normalized) {
        return normalized.contains("다음단계")
                || normalized.contains("다음뭐")
                || normalized.contains("그다음")
                || normalized.contains("이제뭐")
                || normalized.contains("다음에뭐")
                || normalized.contains("다음으로뭐");
    }

    /**
     * 재료 상태 관련 질문
     * TODO: 더 정교화 필요
     */
    private boolean isIngredientStateQuestion(String normalized) {
        return normalized.contains("익었")
                || normalized.contains("상태")
                || normalized.contains("익은거")
                || normalized.contains("다됐어")
                || normalized.contains("다됐니")
                || normalized.contains("완성됐")
                || normalized.contains("언제까지")
                || normalized.contains("색깔어때")
                || normalized.contains("타고있")
                || normalized.contains("탈것같");
    }

    /**
     * 위험 관련 질문
     */
    private boolean isDangerCheckQuestion(String normalized) {
        return normalized.contains("위험")
                || normalized.contains("칼")
                || normalized.contains("불")
                || normalized.contains("뜨거")
                || normalized.contains("주변에")
                || normalized.contains("근처에")
                || normalized.contains("옆에")
                || normalized.contains("혹시무엇")
                || normalized.contains("부딪힐")
                || normalized.contains("닿을만한");
    }
}