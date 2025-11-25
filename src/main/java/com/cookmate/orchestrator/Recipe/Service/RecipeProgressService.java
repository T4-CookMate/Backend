package com.cookmate.orchestrator.Recipe.Service;

import com.cookmate.orchestrator.Recipe.Entity.Recipe;
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

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeProgressService {

    private final RecipeProgressRepository progressRepo;
    private final RecipeRepository recipeRepo;
    private final RecipeStepRepository stepRepo;
    private final UserRepository userRepo;

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

        return Optional.of(progressRepo.save(progress));
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

    @Transactional
    public void cleanup(String sessionKey) {
        if (progressRepo.existsBySessionKey(sessionKey)) {
            Long row = progressRepo.deleteRecipeProgressBySessionKey(sessionKey);
            log.info("{}행 progress가 삭제되었습니다.", row);
        }else{
            log.info("현재 사용자가 진행중인 레시피가 없습니다. cleanup할 progress가 없습니다.");
        }
    }
}
