package com.nutriassistant.nutriassistant_back.repository;

import com.nutriassistant.nutriassistant_back.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByYearAndMonth(Integer year, Integer month);
    boolean existsByYearAndMonth(Integer year, Integer month);
}