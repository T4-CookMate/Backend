package com.cookmate.orchestrator.Ingredient.Entity;

import com.cookmate.orchestrator.Recipe.Entity.StepExpectedState;
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
@Table(name = "ingredient_status")
public class IngredientStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 64)
    @NotNull
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @OneToMany(mappedBy = "currentStatus")
    private Set<StepExpectedState> stepExpectedCurrentStates = new LinkedHashSet<>();

    @OneToMany(mappedBy = "nextStatus")
    private Set<StepExpectedState> stepExpectedNextStates = new LinkedHashSet<>();

}