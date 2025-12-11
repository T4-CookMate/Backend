package com.cookmate.orchestrator.Recipe.Repository;

import com.cookmate.orchestrator.Recipe.Entity.RecipeIngredient;
import com.cookmate.orchestrator.Recipe.Entity.RecipeStep;
import com.cookmate.orchestrator.Recipe.Entity.StepExpectedState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StepExpectedStateRepository extends JpaRepository<StepExpectedState, Long> {
    Optional<StepExpectedState> findByRecipeStepAndRecipeIngredient(RecipeStep currentStep, RecipeIngredient recipeIngredient);
    Optional<StepExpectedState> findByRecipeStep (RecipeStep currentStep);
}
