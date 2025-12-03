package com.cookmate.orchestrator.Recipe.Dto;

import com.cookmate.orchestrator.Ingredient.Entity.Ingredient;
import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import com.cookmate.orchestrator.Recipe.Entity.RecipeIngredient;
import com.cookmate.orchestrator.Recipe.Entity.RecipeStep;
import com.cookmate.orchestrator.Recipe.Entity.Tool;

import java.util.List;

public class RecipeResponse {

    // 레시피 하나 dto
    public record RecipeDto(
            Long recipeId,
            String name,
            Integer totalMinutes,
            Integer level,
            List<String> tags,
            boolean isPrefer
    ) {
        public static RecipeDto from(Recipe recipe, Boolean isPrefer){
            return new RecipeDto(
                    recipe.getId(),
                    recipe.getTitle(),
                    recipe.getTotalMinutes(),
                    recipe.getLevel(),
                    List.of(recipe.getTag1(), recipe.getTag2()),
                    isPrefer
            );
        }
    }

    // 레시피 리스트 dto
    public record RecipeListDto(
            List<RecipeDto> recipe,
            boolean isLast
    ) {}

    public record RecipeDetailDto(
            Long recipeId,
            String name,
            Integer totalMinutes,
            Integer level,
            List<ToolDto> tools,
            List<RecipeIngredientDto> ingredients,
            List<StepDto> steps
    ){
        public static RecipeDetailDto from(Recipe recipe){
            List<ToolDto> toolDtos = recipe.getTools().stream()
                    .map(ToolDto::from)
                    .toList();

            List<RecipeIngredientDto> recipeIngredientDtos = recipe.getRecipeIngredients().stream()
                    .map(RecipeIngredientDto::from)
                    .toList();

            List<StepDto> stepDtos = recipe.getRecipeSteps().stream()
                    .map(StepDto::from)
                    .toList();

            return new RecipeDetailDto(
                    recipe.getId(),
                    recipe.getTitle(),
                    recipe.getTotalMinutes(),
                    recipe.getLevel(),
                    toolDtos,
                    recipeIngredientDtos,
                    stepDtos
            );
        }
    }

    public record ToolDto(
            Long id,
            String name
    ){
        public static ToolDto from(Tool tool) {
            return new ToolDto(
                    tool.getId(),
                    tool.getName()
            );
        }
    }

    public record RecipeIngredientDto(
            Long id,
            String name
    ){
        public static RecipeIngredientDto from(RecipeIngredient recipeIngredient) {
            return new RecipeIngredientDto(
                    recipeIngredient.getId(),
                    recipeIngredient.getIngredient().getName()
            );
        }
    }

    public record StepDto(
            Long id,
            Integer stepIndex,
            String instruction
    ){
        public static StepDto from(RecipeStep recipeStep) {
            return new StepDto(
                    recipeStep.getId(),
                    recipeStep.getStepIndex(),
                    recipeStep.getInstruction()
            );
        }
    }
}
