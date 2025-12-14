package com.cookmate.orchestrator.Recipe.Service;

import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.Exception.GeneralException;
import com.cookmate.orchestrator.Ingredient.Entity.Ingredient;
import com.cookmate.orchestrator.Ingredient.Entity.IngredientRuntimeStatus;
import com.cookmate.orchestrator.Ingredient.Entity.IngredientStatus;
import com.cookmate.orchestrator.Ingredient.Repository.IngredientRepository;
import com.cookmate.orchestrator.Ingredient.Repository.IngredientRuntimeStatusRepository;
import com.cookmate.orchestrator.Recipe.Entity.*;
import com.cookmate.orchestrator.Recipe.Repository.RecipeIngredientRepository;
import com.cookmate.orchestrator.Recipe.Repository.RecipeProgressRepository;
import com.cookmate.orchestrator.Recipe.Repository.StepExpectedStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientStatusService {

    private final RecipeProgressRepository progressRepo;
    private final StepExpectedStateRepository stepExpectedStateRepo;
    private final RecipeIngredientRepository recipeIngredientRepo;
    private final IngredientRepository ingredientRepo;
    private final IngredientRuntimeStatusRepository ingredientRuntimeStatusRepo;

    @Transactional
    public String getCurrentStatus(String sessionKey, String ingredientName) {
        log.info("인식한 재료 이름: {}", ingredientName);
        RecipeProgress progress = progressRepo.findBySessionKey(sessionKey)
                .orElseThrow(() -> new IllegalStateException("세션 진행 정보가 없습니다."));

        RecipeStep currentStep = progress.getCurrentStep();
        if (currentStep == null) {
            return "현재 진행 중인 단계가 없습니다.";
        }

        Ingredient ingredient = ingredientRepo.findByName(ingredientName);
        if (ingredient == null) {
            return ingredientName + "는 이 레시피에 없는 재료 같아요.";
        }
        log.info("DB에서 찾은 재료 이름: {}", ingredient.getName());

        Recipe recipe = progress.getRecipe();
        RecipeIngredient recipeIngredient =
                recipeIngredientRepo.findByIngredientAndRecipe(ingredient, recipe);
        if (recipeIngredient == null) {
            return ingredientName + "는 현재 단계에서는 사용하지 않는 재료예요.";
        }

        StepExpectedState stepExpectedState = stepExpectedStateRepo
                .findByRecipeStepAndRecipeIngredient(currentStep, recipeIngredient)
                .orElseThrow(() -> new GeneralException(
                        ErrorStatus.NO_EXPECTED_STATE,
                        ingredientName + "에 대한 상태 정보가 이 단계에는 정의되어 있지 않아요."
                ));

        IngredientStatus expectedCurrent = stepExpectedState.getCurrentStatus();
        IngredientStatus expectedNext = stepExpectedState.getNextStatus();

        IngredientRuntimeStatus runtimeStatus = ingredientRuntimeStatusRepo
                .findByProgressAndRecipeIngredient(progress, recipeIngredient)
                .orElseThrow(() -> new GeneralException(
                        ErrorStatus._INTERNAL_SERVER_ERROR));

        if (runtimeStatus == null) {
            return ingredientName + "의 현재 상태를 아직 정확히 인식하지 못했어요. " +
                    "조금 더 가까이 보여주실 수 있을까요? " +
                    "레시피 상으로는 지금 " + expectedCurrent.getCode() + " 상태여야 해요.";
        }

        IngredientStatus actual = runtimeStatus.getStatus();

        // 1) 레시피 상 current 상태와 일치
        if (actual.getId().equals(expectedCurrent.getId())) {
            return ingredientName + "은 지금 " + actual.getName() +
                    " 상태입니다. ";
        }

        // 2) 레시피 상 next 상태까지 온 경우
        if (actual.getId().equals(expectedNext.getId())) {
            return ingredientName + "은 이미 " + actual.getName() +
                    " 상태예요. 다음 단계로 넘어가셔도 괜찮아요.";
        }

        // 3) 그 외: 레시피 기대와 다른 상태
        return ingredientName + "은 지금 " + actual.getName() +
                " 상태입니다, 원래" + expectedCurrent.getName() + "상태여야 해요.";

    }
}