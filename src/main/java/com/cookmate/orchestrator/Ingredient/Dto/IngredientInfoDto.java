package com.cookmate.orchestrator.Ingredient.Dto;

import com.cookmate.orchestrator.Ingredient.Entity.IngredientStatus;
import com.cookmate.orchestrator.Recipe.Entity.Location;

public record IngredientInfoDto(
        IngredientStatus status,
        Location location
) {}