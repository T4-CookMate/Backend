package com.cookmate.orchestrator.User.Entity;

import com.cookmate.orchestrator.Recipe.Entity.RecipeProgress;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 30)
    @NotNull
    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Size(max = 30)
    @NotNull
    @Column(name = "email", nullable = false, length = 30)
    private String email;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Size(max = 255)
    @Column(name = "password_hash")
    private String passwordHash;

    @OneToMany(mappedBy = "user")
    private Set<RecipeProgress> recipeProgresses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<UserRecipePreference> userRecipePreferences = new LinkedHashSet<>();

    // provider : google이 들어감
    private String provider;

    // providerId : google 로그인 한 유저의 고유 ID가 들어감
    private String providerId;
}