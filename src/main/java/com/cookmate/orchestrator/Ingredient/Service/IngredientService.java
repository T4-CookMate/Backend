package com.cookmate.orchestrator.Ingredient.Service;

import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.Exception.GeneralException;
import com.cookmate.orchestrator.Ingredient.Dto.IngredientInfoDto;
import com.cookmate.orchestrator.Ingredient.Entity.IngredientRuntimeStatus;
import com.cookmate.orchestrator.Ingredient.Entity.IngredientStatus;
import com.cookmate.orchestrator.Ingredient.Repository.IngredientRuntimeStatusRepository;
import com.cookmate.orchestrator.Recipe.Dto.RecipeRuntimeResponse;
import com.cookmate.orchestrator.Recipe.Entity.*;
import com.cookmate.orchestrator.Recipe.Repository.RecipeProgressRepository;
import com.cookmate.orchestrator.Recipe.Repository.RecipeStepRepository;
import com.cookmate.orchestrator.Recipe.Repository.StepExpectedStateRepository;
import com.cookmate.orchestrator.Recipe.Service.RecipeProgressService;
import com.cookmate.orchestrator.User.Entity.User;
import com.cookmate.orchestrator.VoiceAssist.TTS.TtsRequestEvent;
import com.cookmate.orchestrator.VoiceAssist.TTS.VoiceWebSocketSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientService {

    private final RecipeProgressRepository recipeProgressRepository;
    private final StepExpectedStateRepository stepExpectedStateRepository;
    private final IngredientRuntimeStatusRepository ingredientRuntimeStatusRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final ApplicationEventPublisher publisher;
    private final RecipeProgressService progressService;
    private final VoiceWebSocketSender sender;

    @Transactional
    public RecipeRuntimeResponse updateIngredientsInfo(User user, Map<String, IngredientInfoDto> ingredients) {

        /** 1) 현재 사용자의 레시피 진행상황 조회 */
        RecipeProgress currentRecipeProgress = recipeProgressRepository.findByUserId(user.getId());
        if (currentRecipeProgress == null) {
            log.error("[INGR] NO_PROGRESS userId={}", user.getId());
            throw new GeneralException(ErrorStatus.NO_RECIPE_PROGRESS, "진행 중인 레시피가 없습니다.");
        }

        String sessionKey = currentRecipeProgress.getSessionKey();
        Recipe currentRecipe = currentRecipeProgress.getRecipe();
        RecipeStep currentStep = currentRecipeProgress.getCurrentStep();

        log.warn("[INGR] PROGRESS sessionKey={}, recipeId={}, stepIndex={}, timerSec={}",
                sessionKey,
                currentRecipe != null ? currentRecipe.getId() : null,
                currentStep != null ? currentStep.getStepIndex() : null,
                currentStep != null ? currentStep.getTimer() : null);

        /** 2) 기대 상태 조회 */
        Optional<StepExpectedState> stateOpt = stepExpectedStateRepository.findByRecipeStep(currentStep);

        /** 예상 상태 없는 경우: 타이머 적용 */
        if (stateOpt.isEmpty()) {
            Integer timerSec = currentStep.getTimer();
            log.warn("[INGR] 기대 상태 없음 (stepId={}, timerSec={}, isAutoNextScheduled={})",
                    currentStep != null ? currentStep.getId() : null,
                    timerSec,
                    progressService.isAutoNextScheduled(sessionKey));

            /** 타이머가 정상적으로 정의되어 있는 경우 */
            if (timerSec != null && timerSec > 0) {
                if (!progressService.isAutoNextScheduled(sessionKey)) {
                    log.warn("[INGR] SCHEDULE_AUTO_NEXT now sessionKey={}, expectedStepId={}, timerSec={}",
                            sessionKey, currentStep.getId(), timerSec);
                    progressService.scheduleAutoNextStep(sessionKey, currentStep.getId(), timerSec);
                } else {
                    log.warn("[INGR] SCHEDULE_SKIP_ALREADY_EXISTS stepId={}", currentStep.getId());
                }
                return RecipeRuntimeResponse.from(false, currentRecipeProgress);
            }
            progressService.cancelAutoNext(sessionKey);
            return goToNextStep(sessionKey, currentRecipeProgress, currentRecipe, currentStep);
        }

        /** 예상 상태 있는 경우: 상태 비교 */
        StepExpectedState state = stateOpt.get();

        RecipeIngredient targetRecipeIngredient = state.getRecipeIngredient();
        String ingredientName = targetRecipeIngredient.getIngredient().getName();

//        log.warn("[INGR] EXPECTED_STATE stepId={}, ingredient={}, evidenceHint={}, expectStatus={}, expectLocation={}",
//                currentStep.getId(),
//                ingredientName,
//                state.getEvidenceHint(),
//                state.getNextStatus(),
//                state.getNextLocation());

        IngredientInfoDto info = ingredients.get(ingredientName);
        if (info == null) {
            log.error("[INGR] INGREDIENT_MISSING in payload. expectedName={}, payloadKeys={}",
                    ingredientName, ingredients.keySet());
            throw new GeneralException(ErrorStatus.NO_INGREDIENT, "재료 정보 없음: " + ingredientName);
        }

        IngredientStatus status = info.status();
        Location location = info.location();

//        log.warn("[INGR] OBSERVED ingredient={}, status={}, location={}",
//                ingredientName, status, location);

        IngredientRuntimeStatus runtimeStatus = ingredientRuntimeStatusRepository
                .findByProgressAndRecipeIngredient(currentRecipeProgress, targetRecipeIngredient)
                .orElseThrow(() -> {
                    log.error("[INGR] NO_RUNTIME_STATUS sessionKey={}, ingredient={}", sessionKey, ingredientName);
                    return new GeneralException(ErrorStatus.NO_RUNTIME_STATUS, "실시간 상태가 존재하지 않습니다: " + ingredientName);
                });

        runtimeStatus.setStatus(status);
        runtimeStatus.setLocation(location);

        String evidenceHint = state.getEvidenceHint();
        boolean conditionMet;

        if ("LOCATION".equals(evidenceHint)) {
            conditionMet = state.getNextLocation() != null
                    && state.getNextLocation().equals(runtimeStatus.getLocation());

            log.warn("[INGR] CHECK LOCATION expected={}, actual={}, met={}",
                    state.getNextLocation().getName(),
                    runtimeStatus.getLocation().getName(),
                    conditionMet);

        } else if ("STATUS".equals(evidenceHint)) {
            conditionMet = state.getNextStatus() != null
                    && state.getNextStatus().equals(runtimeStatus.getStatus());

            log.warn("[INGR] CHECK STATUS expected={}, actual={}, met={}",
                    state.getNextStatus().getCode(),
                    runtimeStatus.getStatus().getCode(),
                    conditionMet);

        } else {
            log.error("[INGR] INVALID_EVIDENCE_HINT evidenceHint={}", evidenceHint);
            throw new GeneralException(ErrorStatus.NO_EVIDENCE,
                    "evidence가 설정되지 않았습니다. evidenceHint=" + evidenceHint);
        }

        if (conditionMet) {
            log.warn("[INGR] CONDITION_MET -> GO_NEXT_STEP (현재 step={})", currentStep.getStepIndex());
            progressService.cancelAutoNext(sessionKey);
            return goToNextStep(sessionKey, currentRecipeProgress, currentRecipe, currentStep);
        }

        log.warn("[INGR] CONDITION_NOT_MET -> STAY (현재 step={})", currentStep.getStepIndex());
        return RecipeRuntimeResponse.from(false, currentRecipeProgress);
    }

    private RecipeRuntimeResponse goToNextStep(
            String sessionKey,
            RecipeProgress currentRecipeProgress,
            Recipe currentRecipe,
            RecipeStep currentStep
    ) {
        Optional<RecipeStep> nextStepOpt =
                recipeStepRepository.findNextStep(currentRecipe.getId(), currentStep.getStepIndex());

        if (nextStepOpt.isEmpty()) {
            log.info("nextStep이 없습니다.");

            publisher.publishEvent(new TtsRequestEvent(
                    sessionKey,
                    "레시피가 끝났어요. 수고하셨어요!"
            ));

            WebSocketSession session = sender.getSession(sessionKey);
            if (session == null || !session.isOpen()) {
                log.warn("[WS] END 전송 실패: 세션 없음/닫힘 sessionKey={}", sessionKey);
                return RecipeRuntimeResponse.from(true, currentRecipeProgress);
            }

            try {
                session.sendMessage(new TextMessage("END"));
            } catch (IOException e) {
                log.warn("[WS] END 전송 실패 sessionKey={}", sessionKey, e);
            }

            return RecipeRuntimeResponse.from(
                    true,
                    currentRecipeProgress
            );
        }

        RecipeStep nextStep = nextStepOpt.get();
        currentRecipeProgress.setCurrentStep(nextStep);

        // 그 다음(“다다음”) 스텝 설정 (없으면 null)
        Optional<RecipeStep> afterNextStepOpt =
                recipeStepRepository.findNextStep(currentRecipe.getId(), nextStep.getStepIndex());

        currentRecipeProgress.setNextStep(afterNextStepOpt.orElse(null));

        publisher.publishEvent(new TtsRequestEvent(
                sessionKey,
                "다음 단계예요. " + nextStep.getInstruction()
        ));

        return RecipeRuntimeResponse.from(
                false,
                currentRecipeProgress
        );
    }
}
