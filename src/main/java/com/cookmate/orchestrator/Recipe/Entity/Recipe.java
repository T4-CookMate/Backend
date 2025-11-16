package com.cookmate.orchestrator.Recipe.Entity;

import com.cookmate.orchestrator.User.Entity.UserRecipePreference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "recipe")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 30)
    @NotNull
    @Column(name = "title", nullable = false, length = 30)
    private String title;

    @NotNull
    @Lob
    @Column(name = "description", nullable = false)
    private String description;

    @NotNull
    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes;

    @OneToMany(mappedBy = "recipe")
    private Set<RecipeCategory> recipeCategories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "recipe")
    private Set<RecipeIngredient> recipeIngredients = new LinkedHashSet<>();

    @OneToMany(mappedBy = "recipe")
    private Set<RecipeProgress> recipeProgresses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "recipe")
    private Set<RecipeStep> recipeSteps = new LinkedHashSet<>();

    @OneToMany(mappedBy = "recipe")
    private Set<UserRecipePreference> userRecipePreferences = new LinkedHashSet<>();

}