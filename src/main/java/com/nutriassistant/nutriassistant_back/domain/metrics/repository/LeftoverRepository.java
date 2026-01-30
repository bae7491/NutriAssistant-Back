package com.nutriassistant.nutriassistant_back.domain.metrics.repository;

import com.nutriassistant.nutriassistant_back.domain.metrics.entity.Leftover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeftoverRepository extends JpaRepository<Leftover, Long> {
    boolean existsBySchoolIdAndDateAndMealType(Long schoolId, LocalDate date, String mealType);
    Optional<Leftover> findBySchoolIdAndDateAndMealType(Long schoolId, LocalDate date, String mealType);
    List<Leftover> findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(Long schoolId, String mealType, LocalDate startDate, LocalDate endDate);
}