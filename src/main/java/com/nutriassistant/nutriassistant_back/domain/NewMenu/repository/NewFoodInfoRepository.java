package com.nutriassistant.nutriassistant_back.domain.NewMenu.repository;

import com.nutriassistant.nutriassistant_back.domain.NewMenu.entity.NewFoodInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewFoodInfoRepository extends JpaRepository<NewFoodInfo, Long> {

    Optional<NewFoodInfo> findByFoodCode(String foodCode);

    boolean existsByFoodCode(String foodCode);

    boolean existsByFoodName(String foodName);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(food_code, 9) AS UNSIGNED)) FROM new_food_info WHERE food_code LIKE 'NEWFOOD-%'", nativeQuery = true)
    Integer findMaxFoodCodeNumber();

    List<NewFoodInfo> findByDeletedFalse();

    Page<NewFoodInfo> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
}
