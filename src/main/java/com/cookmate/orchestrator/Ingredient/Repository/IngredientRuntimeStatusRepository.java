package com.cookmate.orchestrator.Ingredient.Repository;

import com.cookmate.orchestrator.Ingredient.Entity.IngredientRuntimeStatus;
import com.cookmate.orchestrator.Recipe.Entity.RecipeIngredient;
import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRuntimeStatusRepository extends JpaRepository<IngredientRuntimeStatus, Long> {
    IngredientRuntimeStatus findByProgressAndRecipeIngredient(RecipeProgress session, RecipeIngredient recipeIngredient);
}
