package com.cookmate.orchestrator.VoiceAssist.Vision;

public record VisionPromptRequest(
        Long recipeId,
        String prompt
) {}