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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public RecipeRuntimeResponse updateIngredientsInfo(User user, Map<String, IngredientInfoDto> ingredients) {
        /** 1) 현재 사용자의 레시피 진행상황 조회 */
        RecipeProgress currentRecipeProgress = recipeProgressRepository.findByUserId(user.getId());
        if (currentRecipeProgress == null) {
            throw new GeneralException(ErrorStatus.NO_RECIPE_PROGRESS, "진행 중인 레시피가 없습니다.");
        }
        String sessionKey = currentRecipeProgress.getSessionKey();

        Recipe currentRecipe = currentRecipeProgress.getRecipe();
        RecipeStep currentStep  = currentRecipeProgress.getCurrentStep();

        /** 2) 이 스텝에서 주시해야 하는 기대 상태(재료 1개 기준) 조회 */
        Optional<StepExpectedState> stateOpt =
                stepExpectedStateRepository.findByRecipeStep(currentStep);

        // 해당 단계에 기대 상태 자체가 없으면 → 10초 뒤에 다음 단계로 그냥 넘어감
        if (stateOpt.isEmpty()) {
            Integer timerSec = currentStep.getTimer();

            if (timerSec != null && timerSec > 0) {
                if (!progressService.isAutoNextScheduled(sessionKey)) {
                    progressService.scheduleAutoNextStep(sessionKey, currentStep.getId(), timerSec);
                }

                // 지금은 그냥 대기 상태 유지
                return RecipeRuntimeResponse.from(
                        false,
                        currentRecipeProgress
                );
            }

            // 타이머 없으면 즉시 다음 단계
            return goToNextStep(sessionKey, currentRecipeProgress, currentRecipe, currentStep);
        }

        // "기대 상태가 있는" 단계에 대한 기존 로직 그대로
        StepExpectedState state = stateOpt.get();

        /** 스텝에서 상태를 주시해야 하는 재료 조회 */
        RecipeIngredient targetRecipeIngredient = state.getRecipeIngredient();
        String ingredientName = targetRecipeIngredient.getIngredient().getName();

        /** 3) 실시간으로 인식된 재료 정보 중에서 해당 재료 정보 가져오기 */
        IngredientInfoDto info = ingredients.get(ingredientName);
        if (info == null) {
            throw new GeneralException(
                    ErrorStatus.NO_INGREDIENT,
                    "재료 정보 없음: " + ingredientName
            );
        }

        IngredientStatus status = info.status();
        Location location = info.location();

        /** 4) 해당 재료의 실시간 상태 엔티티 조회 */
        IngredientRuntimeStatus runtimeStatus = ingredientRuntimeStatusRepository
                .findByProgressAndRecipeIngredient(
                        currentRecipeProgress,
                        targetRecipeIngredient
                )
                .orElseThrow(() -> new GeneralException(
                        ErrorStatus.NO_RUNTIME_STATUS,
                        "실시간 상태가 존재하지 않습니다: " + ingredientName
                ));

        // 최신 상태로 갱신
        runtimeStatus.setStatus(status);
        runtimeStatus.setLocation(location);

        /** 5) 기대 상태와 실제 상태/위치 비교 */
        String evidenceHint = state.getEvidenceHint();
        boolean conditionMet;

        if ("LOCATION".equals(evidenceHint)) {
            // 예상한 위치와 현재 위치가 동일한 경우
            conditionMet = state.getNextLocation() != null
                    && state.getNextLocation().equals(runtimeStatus.getLocation());

        } else if ("STATUS".equals(evidenceHint)) {
            // 예상한 상태와 현재 상태가 동일한 경우
            conditionMet = state.getNextStatus() != null
                    && state.getNextStatus().equals(runtimeStatus.getStatus());

        } else {
            throw new GeneralException(
                    ErrorStatus.NO_EVIDENCE,
                    "evidence가 설정되지 않았습니다. evidenceHint=" + evidenceHint
            );
        }

        // 조건 만족하면 다음 단계로 이동
        if (conditionMet) {
            return goToNextStep(sessionKey, currentRecipeProgress, currentRecipe, currentStep);
        }

        // 아직 조건이 만족되지 않아서 현재 스텝 유지
        return RecipeRuntimeResponse.from(
                false,
                currentRecipeProgress
        );
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
