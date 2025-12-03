package com.cookmate.orchestrator.Recipe.Controller;

import com.cookmate.orchestrator.Common.ApiPayload.ApiResponse;
import com.cookmate.orchestrator.Common.Security.AuthenticatedUser;
import com.cookmate.orchestrator.Recipe.Dto.RecipeResponse;
import com.cookmate.orchestrator.Recipe.Service.RecipeService;
import com.cookmate.orchestrator.User.Entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/recipes")
public class RecipeController {
    private final RecipeService recipeService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchRecipe(@AuthenticatedUser User user, @RequestParam String keyword, @RequestParam Integer page) {
        log.info("🔎 [SEARCH API] keyword='{}', page={}", keyword, page);
        RecipeResponse.RecipeListDto response = recipeService.searchRecipe(user, keyword, page);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/prefer")
    public ResponseEntity<ApiResponse> getPreferRecipe(@AuthenticatedUser User user, @RequestParam int page) {
        RecipeResponse.RecipeListDto response = recipeService.getPreferRecipe(user, page);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @GetMapping("/{recipeId}")
    public ResponseEntity<ApiResponse> getDetailRecipe(@AuthenticatedUser User user, @PathVariable Long recipeId) {
        RecipeResponse.RecipeDetailDto response = recipeService.getDetailRecipe(recipeId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}