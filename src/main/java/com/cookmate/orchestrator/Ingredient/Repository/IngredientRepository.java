package com.cookmate.orchestrator.Ingredient.Repository;

import com.cookmate.orchestrator.Ingredient.Entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Ingredient findByKoreanName(String koreanName);
}
