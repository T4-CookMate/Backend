package com.cookmate.orchestrator.Recipe.Repository;

import com.cookmate.orchestrator.Recipe.Entity.Recipe;
import com.cookmate.orchestrator.User.Entity.UserRecipePreference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRecipePreferencesRepository extends JpaRepository<UserRecipePreference, Long> {
    List<UserRecipePreference> findByUser_IdAndRecipe_IdIn(Long userId, List<Long> recipeIds);
    @Query("""
        SELECT urp.recipe
        FROM UserRecipePreference urp
        WHERE urp.user.id = :userId
    """)
    Page<Recipe> findFavoriteRecipesByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
