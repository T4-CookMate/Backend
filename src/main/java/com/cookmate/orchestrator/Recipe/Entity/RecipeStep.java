package com.cookmate.orchestrator.Recipe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "recipe_steps")
public class RecipeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    @JsonIgnore
    private Recipe recipe;

    @NotNull
    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    @Size(max = 255)
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @NotNull
    @Lob
    @Column(name = "instruction", nullable = false)
    private String instruction;

    @Column(name = "timer")
    private Integer timer;

    @OneToMany(mappedBy = "currentStep")
    private Set<RecipeProgress> recipeProgresses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "step")
    private Set<StepAction> stepActions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "recipeStep")
    private Set<StepExpectedState> stepExpectedStates = new LinkedHashSet<>();

}