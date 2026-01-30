package com.nutriassistant.nutriassistant_back.domain.metrics.repository;

import com.nutriassistant.nutriassistant_back.domain.metrics.entity.SkipMeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SkipMealRepository extends JpaRepository<SkipMeal, Long> {
    boolean existsBySchoolIdAndDateAndMealType(Long schoolId, LocalDate date, String mealType);
    Optional<SkipMeal> findBySchoolIdAndDateAndMealType(Long schoolId, LocalDate date, String mealType);
    List<SkipMeal> findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(Long schoolId, String mealType, LocalDate startDate, LocalDate endDate);
}