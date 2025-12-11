package com.cookmate.orchestrator.Ingredient.Entity;

import com.cookmate.orchestrator.Recipe.Entity.ActionIngredient;
import com.cookmate.orchestrator.Recipe.Entity.RecipeIngredient;
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
@Table(name = "ingredients")
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "korean_name", nullable = false)
    private String koreanName;

    @OneToMany(mappedBy = "ingredient")
    private Set<ActionIngredient> actionIngredients = new LinkedHashSet<>();

    @OneToMany(mappedBy = "ingredient")
    private Set<RecipeIngredient> recipeIngredients = new LinkedHashSet<>();

}