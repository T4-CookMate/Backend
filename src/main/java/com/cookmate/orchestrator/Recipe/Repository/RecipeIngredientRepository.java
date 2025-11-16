package com.cookmate.orchestrator.Recipe.Repository;

import com.cookmate.orchestrator.Ingredient.Entity.Ingredient;
import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import com.cookmate.orchestrator.Recipe.Entity.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    RecipeIngredient findByIngredientAndRecipe(Ingredient ingredient, Recipe recipe);
}
