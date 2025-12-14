package com.cookmate.orchestrator.Recipe.Entity;

import com.cookmate.orchestrator.User.Entity.UserRecipePreference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    @Lob
    @NotNull
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String prompt;

    @NotNull
    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes;

    @NotNull
    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "tag1")
    private String tag1;

    @Column(name = "tag2")
    private String tag2;

    @OneToMany(mappedBy = "recipe",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Tool> tools = new ArrayList<>();

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