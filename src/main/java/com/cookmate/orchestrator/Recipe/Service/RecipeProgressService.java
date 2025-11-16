package com.cookmate.orchestrator.Recipe.Service;

import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;
import com.cookmate.orchestrator.Recipe.Entity.RecipeStep;
import com.cookmate.orchestrator.Recipe.Repository.RecipeProgressRepository;
import com.cookmate.orchestrator.Recipe.Repository.RecipeRepository;
import com.cookmate.orchestrator.Recipe.Repository.RecipeStepRepository;
import com.cookmate.orchestrator.User.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipeProgressService {

    private final RecipeProgressRepository progressRepo;
    private final RecipeRepository recipeRepo;
    private final RecipeStepRepository stepRepo;
    private final UserRepository userRepo;

    @Transactional
    public RecipeProgress startSession(Long userId, Long recipeId, String sessionKey) {

        // 이미 이 유저가 이 레시피를 진행 중이면 에러
        if (progressRepo.existsByUserIdAndRecipeId(userId, recipeId)) {
            throw new IllegalStateException("이미 진행 중인 레시피입니다. resumeSession()을 사용하세요.");
        }

        // 레시피 / 첫 단계 조회
        Recipe recipe = recipeRepo.getReferenceById(recipeId);

        RecipeStep firstStep = stepRepo
                .findFirstByRecipeOrderByStepIndexAsc(recipe)
                .orElseThrow(() -> new IllegalStateException("레시피 단계가 없습니다."));

        RecipeStep nextStep = stepRepo
                .findNextStep(recipeId, firstStep.getStepIndex())
                .orElse(firstStep);   // 마지막 단계면 자기 자신

        // 새 progress 생성
        RecipeProgress progress = new RecipeProgress();
        progress.setUser(userRepo.getReferenceById(userId));
        progress.setRecipe(recipe);
        progress.setCurrentStep(firstStep);
        progress.setNextStep(nextStep);
        progress.setSessionKey(sessionKey);

        return progressRepo.save(progress);
    }

    @Transactional(readOnly = true)
    public String getNextStep (String sessionKey){
        RecipeProgress progress = progressRepo.findBySessionKey(sessionKey)
                .orElseThrow(() -> new IllegalStateException("세션 진행 정보가 없습니다."));

        RecipeStep nextStep = progress.getNextStep();
        if (nextStep == null) {
            return "이미 마지막 단계까지 진행하셨어요.";
        }

        // title + instruction 조합해서 음성용 텍스트로 변환
        String title = nextStep.getTitle();
        String instruction = nextStep.getInstruction();

        return "다음 단계는 " + title + " 입니다. " + instruction;
    }
}
