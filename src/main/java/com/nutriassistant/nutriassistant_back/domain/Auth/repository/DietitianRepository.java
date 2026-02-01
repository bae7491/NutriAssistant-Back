package com.nutriassistant.nutriassistant_back.domain.Auth.repository;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DietitianRepository extends JpaRepository<Dietitian, Long> {

    boolean existsByUsername(String username);

    Optional<Dietitian> findByUsername(String username);
}
