package com.cookmate.orchestrator.Recipe.Repository;

import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipeProgressRepository extends JpaRepository<RecipeProgress, Long> {
    Boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);
    Boolean existsBySessionKey (String sessionKey);
    Optional<RecipeProgress> findBySessionKey(String sessionKey);
    Long deleteRecipeProgressBySessionKey(String sessionKey);
}