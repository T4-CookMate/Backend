package com.cookmate.orchestrator.Recipe.Service;

import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.Exception.GeneralException;
import com.cookmate.orchestrator.Ingredient.Entity.IngredientRuntimeStatus;
import com.cookmate.orchestrator.Ingredient.Entity.IngredientStatus;
import com.cookmate.orchestrator.Ingredient.Repository.IngredientRuntimeStatusRepository;
import com.cookmate.orchestrator.Ingredient.Repository.IngredientStatusRepository;
import com.cookmate.orchestrator.Recipe.Dto.RecipeRuntimeResponse;
import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import com.cookmate.orchestrator.Recipe.Entity.RecipeIngredient;
import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;
import com.cookmate.orchestrator.Recipe.Entity.RecipeStep;
import com.cookmate.orchestrator.Recipe.Repository.RecipeProgressRepository;
import com.cookmate.orchestrator.Recipe.Repository.RecipeRepository;
import com.cookmate.orchestrator.Recipe.Repository.RecipeStepRepository;
import com.cookmate.orchestrator.User.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeProgressService {

    private final RecipeProgressRepository progressRepo;
    private final RecipeRepository recipeRepo;
    private final RecipeStepRepository stepRepo;
    private final UserRepository userRepo;
    private final IngredientStatusRepository ingredientStatusRepository;
    private final IngredientRuntimeStatusRepository ingredientRuntimeStatusRepository;

    @Transactional
    public Optional<RecipeProgress> startSession(Long userId, Long recipeId, String sessionKey) {

        // 이미 이 유저가 이 레시피를 진행 중이면 빈 Optional 반환
        if (progressRepo.existsByUserIdAndRecipeId(userId, recipeId)) {
            log.info("Session already started");
            return Optional.empty();
        }

        // 레시피 / 첫 단계 조회
        Recipe recipe = recipeRepo.getReferenceById(recipeId);

        RecipeStep firstStep = stepRepo
                .findFirstByRecipeOrderByStepIndexAsc(recipe)
                .orElseThrow(() -> new IllegalStateException("레시피 단계가 없습니다."));

        Optional<RecipeStep> nextStepOpt =
                stepRepo.findNextStep(recipeId, firstStep.getStepIndex());

        // 새 progress 생성
        RecipeProgress progress = new RecipeProgress();
        progress.setUser(userRepo.getReferenceById(userId));
        progress.setRecipe(recipe);
        progress.setCurrentStep(firstStep);
        progress.setNextStep(nextStepOpt.orElse(null));   // 마지막 단계면 null
        progress.setSessionKey(sessionKey);

        // progress 저장
        progressRepo.save(progress);

        // UNALLOCATED 상태 엔티티 조회
        IngredientStatus unallocatedStatus = ingredientStatusRepository
                .findByCode("UNALLOCATED")
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOT_FOUND, "UNALLOCATED 상태가 IngredientStatus 테이블에 없습니다."));

        // 레시피에 사용되는 재료들에 대해 runtime status 초기화
        List<IngredientRuntimeStatus> runtimeList = new ArrayList<>();

        for (RecipeIngredient recipeIngredient : recipe.getRecipeIngredients()) {

            IngredientRuntimeStatus runtimeStatus = new IngredientRuntimeStatus();
            runtimeStatus.setProgress(progress);                 // 이 세션의 진행상황
            runtimeStatus.setRecipeIngredient(recipeIngredient); // 어떤 재료에 대한 상태인지
            runtimeStatus.setStatus(unallocatedStatus);          // 초기 상태 = UNALLOCATED
            runtimeStatus.setLocation(null);                     // 위치는 아직 모름

            runtimeList.add(runtimeStatus);
        }

        ingredientRuntimeStatusRepository.saveAll(runtimeList);

        return Optional.of(progressRepo.save(progress));
    }

    @Transactional
    public String startStep(String sessionKey){
        Optional<RecipeProgress> progressOpt = progressRepo.findBySessionKey(sessionKey);
        if (progressOpt.isEmpty()) {
            log.info("현재 진행 중인 레시피가 없습니다.");
            throw new GeneralException(ErrorStatus._NOT_FOUND);
        }

        RecipeProgress progress = progressOpt.get();
        RecipeStep currentStep = progress.getCurrentStep();

        String title = currentStep.getTitle();
        String instruction = currentStep.getInstruction();

        return "첫 단계는 " + title + " 입니다. " + instruction;
    }

    /**
     * 다음 단계 질문 응답 생성 후 이동 여부 묻기
     */
    @Transactional
    public String nextStepQuestionResponse (String sessionKey){
        RecipeProgress progress = progressRepo.findBySessionKey(sessionKey)
                .orElseThrow(() -> new IllegalStateException("세션 진행 정보가 없습니다."));

        // 현재 레시피 조회
        Recipe currentRecipe = progress.getRecipe();
        RecipeStep currentStep = progress.getCurrentStep();

        Optional<RecipeStep> nextStepOpt =
                stepRepo.findNextStep(currentRecipe.getId(), currentStep.getStepIndex());

        if (nextStepOpt.isEmpty()) {
            log.info("nextStep이 없습니다.");
            return "마지막 단계입니다.";
        }
        RecipeStep nextStep = nextStepOpt.get();

        // title + instruction 조합해서 음성용 텍스트로 변환
        String title = nextStep.getTitle();
        String instruction = nextStep.getInstruction();
        return "다음 단계는 " + title + " 입니다. " + instruction + "다음 단계로 넘어갈까요?";
    }

    /**
     * 실제 단계 이동 (set)
     */
    @Transactional
    public String setNextStep(String sessionKey){
        RecipeProgress progress = progressRepo.findBySessionKey(sessionKey)
                .orElseThrow(() -> new IllegalStateException("세션 진행 정보가 없습니다."));

        // 현재 레시피 조회
        Recipe currentRecipe = progress.getRecipe();
        RecipeStep currentStep = progress.getCurrentStep();

        Optional<RecipeStep> nextStepOpt =
                stepRepo.findNextStep(currentRecipe.getId(), currentStep.getStepIndex());

        if (nextStepOpt.isEmpty()) {
            log.info("nextStep이 없습니다.");
            return "마지막 단계입니다.";
        }
        RecipeStep nextStep = nextStepOpt.get();

        // 현재 스텝을 다음 스텝으로 저장
        progress.setCurrentStep(nextStep);

        // 그 다음(“다다음”) 스텝 설정 (없으면 null)
        Optional<RecipeStep> afterNextStepOpt =
                stepRepo.findNextStep(currentRecipe.getId(), nextStep.getStepIndex());

        if(afterNextStepOpt.isPresent()) {
            progress.setNextStep(afterNextStepOpt.orElse(null));
        }
        String title = nextStep.getTitle();
        String instruction = nextStep.getInstruction();
        return "다음 단계는 " + title + " 입니다. "+ instruction;
    }

    @Transactional
    public void cleanup(String sessionKey) {

        Optional<RecipeProgress> progressOpt = progressRepo.findBySessionKey(sessionKey);

        if (progressOpt.isEmpty()) {
            log.info("현재 사용자가 진행중인 레시피가 없습니다. cleanup할 progress가 없습니다.");
            return;
        }

        RecipeProgress progress = progressOpt.get();

        // 1) 해당 progress를 가진 모든 IngredientRuntimeStatus 삭제
        int deletedRuntimeRows = ingredientRuntimeStatusRepository
                .deleteAllByProgress(progress);

        log.info("IngredientRuntimeStatus {}행이 삭제되었습니다.", deletedRuntimeRows);

        // 2) progress 삭제
        Long deletedProgressRows = progressRepo.deleteRecipeProgressBySessionKey(sessionKey);

        log.info("RecipeProgress {}행이 삭제되었습니다.", deletedProgressRows);
    }
}
