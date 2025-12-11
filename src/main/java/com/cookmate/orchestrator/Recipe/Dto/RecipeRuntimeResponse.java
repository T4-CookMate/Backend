package com.cookmate.orchestrator.Recipe.Dto;

import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;

public record RecipeRuntimeResponse(
        Boolean isEnd,
        Integer stepIndex
) {
    public static RecipeRuntimeResponse from(Boolean isEnd, RecipeProgress currentProgress){
        return new RecipeRuntimeResponse(
                isEnd,
                currentProgress.getCurrentStep().getStepIndex()
        );
    }
}
