package com.nutriassistant.nutriassistant_back.Auth.repository;

import com.nutriassistant.nutriassistant_back.Auth.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {

    Optional<School> findByDietitian_Id(Long dietitianId);
}
