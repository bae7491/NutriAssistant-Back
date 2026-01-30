package com.nutriassistant.nutriassistant_back.domain.MealPlan.repository;

import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MenuCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MenuCostRepository extends JpaRepository<MenuCost, Long> {

    Optional<MenuCost> findByMenuName(String menuName);

    List<MenuCost> findByCurrentYear(Integer currentYear);

    @Query("SELECT m.menuName, m.price FROM MenuCost m")
    List<Object[]> findAllMenuNameAndPrice();

    boolean existsByMenuName(String menuName);
}