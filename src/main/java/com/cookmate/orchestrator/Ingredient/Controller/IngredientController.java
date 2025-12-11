package com.cookmate.orchestrator.Ingredient.Controller;

import com.cookmate.orchestrator.Common.ApiPayload.ApiResponse;
import com.cookmate.orchestrator.Common.Security.AuthenticatedUser;
import com.cookmate.orchestrator.Ingredient.Dto.IngredientInfoDto;
import com.cookmate.orchestrator.Ingredient.Dto.IngredientInfoRequest;
import com.cookmate.orchestrator.Ingredient.Service.IngredientInfoMapper;
import com.cookmate.orchestrator.Ingredient.Service.IngredientService;
import com.cookmate.orchestrator.Recipe.Dto.RecipeRuntimeResponse;
import com.cookmate.orchestrator.User.Entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;
    private final IngredientInfoMapper ingredientInfoMapper;

    @PostMapping("/info")
    public ResponseEntity<ApiResponse> updateIngredientStatus(
            @AuthenticatedUser User user,
            @RequestBody Map<String, IngredientInfoRequest> ingredientInfos
    ) {
        Map<String, IngredientInfoDto> dtoMap = ingredientInfoMapper.toDtoMap(ingredientInfos);
        RecipeRuntimeResponse response = ingredientService.updateIngredientsInfo(user, dtoMap);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
