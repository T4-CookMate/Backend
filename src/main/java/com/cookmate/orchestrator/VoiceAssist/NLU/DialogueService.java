package com.cookmate.orchestrator.VoiceAssist.NLU;

import com.cookmate.orchestrator.Recipe.Service.IngredientStatusService;
import com.cookmate.orchestrator.Recipe.Service.RecipeProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogueService {

    private final RecipeProgressService progressService;
    private final IngredientStatusService ingredientStatusService;

    /**
     * STT + NLU까지 끝난 뒤, 실제 사용자에게 들려줄 답변 문장을 만드는 메서드
     */
    public String handleIntent(String sessionId, IntentResult intentResult) {
        IntentType type = intentResult.type();
        Map<String, String> slots = intentResult.slots();

        return switch (type) {
            case NEXT_STEP -> handleNextStep(sessionId);
            case INGREDIENT_STATE -> handleIngredientState(sessionId, slots);
            case DANGER_CHECK -> handleDangerCheck(sessionId, slots);
            case UNKNOWN -> handleUnknown(sessionId, intentResult.originalText());
        };
    }

    /**
     * 현재 요리 진행 상황 체크 후 응답 생성
     */
    private String handleNextStep(String sessionId) {
        return progressService.getNextStep(sessionId);
    }

    /**
     * 현재 재료 상태 체크 후 응답 생성
     */
    private String handleIngredientState(String sessionId, Map<String, String> slots) {
        String ingredient = slots.getOrDefault("ingredient", "재료");
        return ingredientStatusService.getCurrentStatus(sessionId, ingredient);
    }

    /**
     * 위험 요소 체크 후 응답 생성
     */
    private String handleDangerCheck(String sessionId, Map<String, String> slots) {
        String direction = slots.getOrDefault("direction", "주변");

        // TODO: vision 서비스에서 위험 요소 조회

        // 예시 답변 (위험 있다고 가정)
        if ("left".equals(direction)) {
            return "왼쪽에 칼이 놓여 있어요. 손이 닿지 않도록 조금 더 안쪽으로 밀어둘게요.";
        } else if ("right".equals(direction)) {
            return "오른쪽에 뜨거운 팬이 있어요. 손이 닿지 않도록 조심해 주세요.";
        } else if ("front".equals(direction)) {
            return "앞쪽에 도마 끝이 튀어나와 있어요. 배나 허리에 걸리지 않도록 살짝 안으로 밀어둘게요.";
        } else if ("back".equals(direction)) {
            return "뒤쪽에는 별다른 위험 요소가 감지되지 않았어요. 그래도 돌아설 때 천천히 움직여 주세요.";
        }

        // 방향 언급이 없을 때: 전체 주변 위험 체크
        return "지금 주변에 특별한 위험 요소는 보이지 않아요. 그래도 뜨거운 조리도구에 손이 닿지 않게 조심해 주세요.";
    }

    /**
     * 질문 한 번 더 요청
     */
    private String handleUnknown(String sessionId, String originalText) {
        log.info("[handleUnknown]: sessionId = {},originalText = {}", sessionId, originalText);
        return "다시 한 번 말씀해주시겠어요?";
    }
}