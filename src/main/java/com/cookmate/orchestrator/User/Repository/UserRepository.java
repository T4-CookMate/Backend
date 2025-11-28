package com.cookmate.orchestrator.User.Repository;

import com.cookmate.orchestrator.User.Entity.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(@NotNull Long userId);
    Optional<User> findByEmail(@NotNull String email);
}
