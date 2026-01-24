package com.nutriassistant.nutriassistant_back.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.entity.Report;
import com.nutriassistant.nutriassistant_back.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<Report> findByYearAndMonth(Integer year, Integer month) {
        return reportRepository.findByYearAndMonth(year, month);
    }

    public JsonNode getReportDataAsJson(Report report) {
        try {
            return objectMapper.readTree(report.getReportData());
        } catch (JsonProcessingException e) {
            log.error("❌ JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}