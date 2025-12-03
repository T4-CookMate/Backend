package com.cookmate.orchestrator.Recipe.Repository;

import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findById(Long recipeId);
    Page<Recipe> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
}
