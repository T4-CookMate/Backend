package com.cookmate.orchestrator.Recipe.Repository;

import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import com.cookmate.orchestrator.Recipe.Entity.RecipeStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {
    // 레시피의 첫 단계 (stepIndex 오름차순)
    Optional<RecipeStep> findFirstByRecipeOrderByStepIndexAsc(Recipe recipe);

    // 다음 단계 조회
    @Query("""
        SELECT rs FROM RecipeStep rs
        WHERE rs.recipe.id = :recipeId AND rs.stepIndex = :stepIndex + 1
        """)
    Optional<RecipeStep> findNextStep(@Param("recipeId") Long recipeId,
                                      @Param("stepIndex") int stepIndex);
}
