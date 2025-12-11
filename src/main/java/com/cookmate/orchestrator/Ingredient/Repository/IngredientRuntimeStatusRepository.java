package com.cookmate.orchestrator.Ingredient.Repository;

import com.cookmate.orchestrator.Ingredient.Entity.IngredientRuntimeStatus;
import com.cookmate.orchestrator.Recipe.Entity.RecipeIngredient;
import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientRuntimeStatusRepository extends JpaRepository<IngredientRuntimeStatus, Long> {
    Optional<IngredientRuntimeStatus> findByProgressAndRecipeIngredient(RecipeProgress session, RecipeIngredient recipeIngredient);

    @Modifying
    @Query("DELETE FROM IngredientRuntimeStatus r WHERE r.progress = :progress")
    int deleteAllByProgress(@Param("progress") RecipeProgress progress);
}
