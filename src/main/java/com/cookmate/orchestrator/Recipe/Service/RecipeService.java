package com.cookmate.orchestrator.Recipe.Service;

import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.Exception.GeneralException;
import com.cookmate.orchestrator.Recipe.Dto.RecipeResponse;
import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import com.cookmate.orchestrator.User.Entity.User;
import com.cookmate.orchestrator.Recipe.Repository.RecipeRepository;
import com.cookmate.orchestrator.Recipe.Repository.UserRecipePreferencesRepository;
import com.cookmate.orchestrator.User.Entity.UserRecipePreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final UserRecipePreferencesRepository userRecipePreferencesRepository;

    @Transactional(readOnly = true)
    public RecipeResponse.RecipeListDto searchRecipe(User user, String keyword, Integer page){
        Long userId = user.getId();
        keyword = keyword.trim();

        log.info("Searching recipe for keyword {}, page: {}", keyword, page);

        Pageable pageable = PageRequest.of(page, 4);
        Page<Recipe> recipePage =
                recipeRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        log.info("Search recipe for " + keyword + recipePage.getNumberOfElements() + "개");
        if (!recipePage.hasContent()) {
            if (page == 0) {
                throw new GeneralException(ErrorStatus._NOT_FOUND, "해당 키워드에 대한 레시피가 존재하지 않습니다.");
            }
            // 그 외 페이지는 그냥 빈 리스트 리턴
            return new RecipeResponse.RecipeListDto(List.of(), 0, true);
        }
        List<Recipe> recipes = recipePage.getContent();

        // 현재 페이지 레시피들의 id 리스트 뽑기
        List<Long> recipeIds = recipes.stream()
                .map(Recipe::getId)
                .toList();

        // 3) 로그인 한 사용자라면, 그 사용자가 이 레시피들 중 즐겨찾기한 것들만 한 번에 조회
        final Set<Long> preferredRecipeIds = new HashSet<>();

        if (!recipeIds.isEmpty()) {
            List<UserRecipePreference> preferences =
                    userRecipePreferencesRepository.findByUser_IdAndRecipe_IdIn(userId, recipeIds);

            preferredRecipeIds.addAll(
                    preferences.stream()
                            .map(pref -> pref.getRecipe().getId())
                            .toList()
            );
        }

        // DTO 변환하면서 isPrefer 채우기
        List<RecipeResponse.RecipeDto> RecipeDtos = recipes.stream()
                .map(recipe -> {
                    boolean isPrefer = preferredRecipeIds.contains(recipe.getId());

                    return RecipeResponse.RecipeDto.from(
                            recipe,
                            isPrefer
                    );
                })
                .toList();

        return new RecipeResponse.RecipeListDto(
                RecipeDtos,
                RecipeDtos.size(),
                recipePage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public RecipeResponse.RecipeListDto getPreferRecipe(User user, int page){
        Long userId = user.getId();
        Pageable pageable = PageRequest.of(page, 4);
        Page<Recipe> recipePage =
                userRecipePreferencesRepository.findFavoriteRecipesByUserId(userId, pageable);

        if (!recipePage.hasContent()) {
            if (page == 0) {
                throw new GeneralException(ErrorStatus._NOT_FOUND, "해당 사용자가 즐겨찾기한 레시피가 없습니다.");
            }
            // 그 외 페이지는 그냥 빈 리스트 리턴
            return new RecipeResponse.RecipeListDto(List.of(), 0, true);
        }

        List<RecipeResponse.RecipeDto> RecipeDtos = recipePage.getContent().stream()
                .map(recipe -> RecipeResponse.RecipeDto.from(
                        recipe,
                        true
                ))
                .toList();

        return new RecipeResponse.RecipeListDto(
                RecipeDtos,
                RecipeDtos.size(),
                recipePage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public RecipeResponse.RecipeDetailDto getDetailRecipe(Long recipeId){
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() ->
                        new GeneralException(ErrorStatus._NOT_FOUND, "해당 레시피가 존재하지 않습니다.")
                );

        return RecipeResponse.RecipeDetailDto.from(recipe);
    }

}
