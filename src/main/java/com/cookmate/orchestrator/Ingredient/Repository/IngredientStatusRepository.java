package com.cookmate.orchestrator.Ingredient.Repository;

import com.cookmate.orchestrator.Ingredient.Entity.IngredientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientStatusRepository extends JpaRepository<IngredientStatus, Long> {
    Optional<IngredientStatus> findByCode(String code);
}
