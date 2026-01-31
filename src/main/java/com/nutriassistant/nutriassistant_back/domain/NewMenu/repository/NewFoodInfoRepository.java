package com.nutriassistant.nutriassistant_back.domain.NewMenu.repository;

import com.nutriassistant.nutriassistant_back.domain.NewMenu.entity.NewFoodInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewFoodInfoRepository extends JpaRepository<NewFoodInfo, Long> {

    Optional<NewFoodInfo> findByFoodCode(String foodCode);

    boolean existsByFoodCode(String foodCode);
}
