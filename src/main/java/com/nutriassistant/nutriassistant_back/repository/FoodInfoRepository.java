package com.nutriassistant.nutriassistant_back.repository;

import com.nutriassistant.nutriassistant_back.entity.FoodInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface FoodInfoRepository extends JpaRepository<FoodInfo, Long> {
    Page<FoodInfo> findByUpdatedAtAfter(Instant since, Pageable pageable);
    Page<FoodInfo> findAll(Pageable pageable);
}
