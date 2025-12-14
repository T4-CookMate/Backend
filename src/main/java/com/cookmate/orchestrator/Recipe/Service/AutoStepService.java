package com.cookmate.orchestrator.Recipe.Service;

import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.Exception.GeneralException;
import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;
import com.cookmate.orchestrator.Recipe.Entity.RecipeStep;
import com.cookmate.orchestrator.Recipe.Repository.RecipeProgressRepository;
import com.cookmate.orchestrator.Recipe.Repository.RecipeStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoStepService {

    private final RecipeProgressRepository progressRepo;
    private final RecipeStepRepository stepRepo;

    @Transactional
    public void runAutoNext(String sessionKey, Long expectedStepId) {

        RecipeProgress progress = progressRepo.findBySessionKey(sessionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND));

        // 아직 같은 step일 때만 다음 단계로
        if (!progress.getCurrentStep().getId().equals(expectedStepId)) {
            log.info("[AUTO NEXT] step changed. skip session={}", sessionKey);
            return;
        }

        Recipe currentRecipe = progress.getRecipe();
        RecipeStep currentStep = progress.getCurrentStep();

        RecipeStep nextStep = stepRepo
                .findNextStep(currentRecipe.getId(), currentStep.getStepIndex())
                .orElse(null);

        if (nextStep == null) {
            log.info("[TIMER] last step. progressId={}", progress.getId());
            return;
        }

        progress.setCurrentStep(nextStep);

        RecipeStep afterNext = stepRepo
                .findNextStep(currentRecipe.getId(), nextStep.getStepIndex())
                .orElse(null);

        // null도 세팅해서 이전 값 남는 거 방지
        progress.setNextStep(afterNext);
    }
}
