package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.Leftover;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.SkipMeal;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.LeftoverRepository;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.SkipMealRepository;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.dto.MonthlyOpsDocDto;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity.FileAttachment;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity.MonthlyOpsDoc;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity.ReportStatus;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.repository.FileAttachmentRepository;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.repository.MonthlyOpsDocRepository;

import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;
import com.nutriassistant.nutriassistant_back.domain.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyOpsDocService {

    private final MonthlyOpsDocRepository monthlyOpsDocRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final SkipMealRepository skipMealRepository;
    private final LeftoverRepository leftoverRepository;
    private final ReviewRepository reviewRepository;

    // =========================================================================
    // 1. [Create] Create Operation Document (Stats -> AI Analysis -> DB Save)
    // =========================================================================
    @Transactional
    public MonthlyOpsDocDto.Response createMonthlyOpsDoc(MonthlyOpsDocDto.CreateRequest request) {

        // 1-1. Prevent Duplicate Creation
        if (monthlyOpsDocRepository.existsBySchoolIdAndYearAndMonth(
                request.getSchool_id(), request.getYear(), request.getMonth())) {
            throw new IllegalArgumentException("An operation document for this month already exists.");
        }

        // 1-2. Calculate Date Range
        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // 1-3. Fetch Statistics Data from DB
        log.info("📊 Fetching statistics data: {}-{}", request.getYear(), request.getMonth());

        List<SkipMeal> lunchSkips = skipMealRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "LUNCH", startDate, endDate);
        List<Leftover> lunchLeftovers = leftoverRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "LUNCH", startDate, endDate);

        List<SkipMeal> dinnerSkips = skipMealRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "DINNER", startDate, endDate);
        List<Leftover> dinnerLeftovers = leftoverRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "DINNER", startDate, endDate);

        List<Review> monthlyReviews = reviewRepository.findBySchoolIdAndCreatedAtBetween(
                request.getSchool_id(), startDateTime, endDateTime);

        log.info("   Lunch Data: {}, Dinner Data: {}", lunchSkips.size(), dinnerSkips.size());
        log.info("   Collected Reviews: {}", monthlyReviews.size());

        // 1-4. Construct Payload for FastAPI Request
        Map<String, Object> fastApiPayload = buildFastApiPayload(
                request,
                lunchSkips, lunchLeftovers,
                dinnerSkips, dinnerLeftovers,
                monthlyReviews
        );

        // 1-5. Call FastAPI (Request AI Analysis)
        Map<String, Object> analyzedResult;
        try {
            log.info("🤖 Starting FastAPI Analysis Request: /api/reports/monthly");
            analyzedResult = restClient.post()
                    .uri("/api/reports/monthly")
                    // [Fix] 실제 토큰 값이나 로직으로 교체 필요
                    .header("Authorization", "Bearer temp_token_value")
                    .body(fastApiPayload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            log.info("✅ AI Analysis Completed");
        } catch (Exception e) {
            log.error("❌ FastAPI Analysis Request Failed", e);
            throw new RuntimeException("AI Analysis Server Error: " + e.getMessage());
        }

        // 1-6. Convert Result to JSON
        String reportContentJson;
        try {
            Object dataObj = analyzedResult.get("data") != null ? analyzedResult.get("data") : analyzedResult;
            reportContentJson = objectMapper.writeValueAsString(dataObj);
        } catch (Exception e) {
            log.error("❌ JSON Conversion Failed", e);
            throw new RuntimeException("JSON Conversion Error: " + e.getMessage());
        }

        // 1-7. Save to DB
        MonthlyOpsDoc doc = MonthlyOpsDoc.builder()
                .schoolId(request.getSchool_id())
                .title(request.getTitle())
                .year(request.getYear())
                .month(request.getMonth())
                .status(ReportStatus.COMPLETED)
                .reportData(reportContentJson)
                .build();

        MonthlyOpsDoc savedDoc = monthlyOpsDocRepository.save(doc);
        log.info("💾 Report saved to DB: ID={}", savedDoc.getId());

        return getMonthlyOpsDocDetail(savedDoc.getId());
    }

    /**
     * Constructs the data structure to send to FastAPI.
     * [Fix] Resolved lambda compilation errors by using explicit lambdas instead of method references.
     */
    private Map<String, Object> buildFastApiPayload(
            MonthlyOpsDocDto.CreateRequest request,
            List<SkipMeal> lunchSkips, List<Leftover> lunchLeftovers,
            List<SkipMeal> dinnerSkips, List<Leftover> dinnerLeftovers,
            List<Review> reviews) {

        Map<String, Object> payload = new HashMap<>();

        // Required field
        payload.put("school_id", request.getSchool_id());

        // Meta Information (Snake Case)
        payload.put("user_name", "Admin");
        payload.put("year", request.getYear());
        payload.put("month", request.getMonth());
        payload.put("target_group", "STUDENT");

        // [Fix] Lambda Expression Error Resolved here
        // Collectors.toMap에서 메서드 참조(Leftover::getDate) 대신 명시적 람다를 사용하여
        // 컴파일러가 타입을 확실히 추론할 수 있도록 수정했습니다.
        Map<LocalDate, Double> lunchLeftoverMap = lunchLeftovers.stream()
                .collect(Collectors.toMap(
                        leftover -> leftover.getDate(),                          // Key
                        leftover -> leftover.getAmountKg() != null ? leftover.getAmountKg() : 0.0, // Value (Null check)ㅊ
                        (existing, replacement) -> replacement                   // Merge function (중복 시 최신값)
                ));

        Map<LocalDate, Double> dinnerLeftoverMap = dinnerLeftovers.stream()
                .collect(Collectors.toMap(
                        leftover -> leftover.getDate(),
                        leftover -> leftover.getAmountKg() != null ? leftover.getAmountKg() : 0.0,
                        (existing, replacement) -> replacement
                ));

        // [Fix] daily_analyses Generation
        List<Map<String, Object>> dailyAnalysesList = new ArrayList<>();

        // Process Lunch Skips -> Daily Analysis
        for (SkipMeal skip : lunchSkips) {
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("date", skip.getDate().toString());
            analysis.put("menu_name", "Lunch Menu");
            analysis.put("meal_type", "LUNCH");
            int served = skip.getTotalStudents() - skip.getSkippedCount();
            analysis.put("served_count", served);
            analysis.put("leftover_amount", lunchLeftoverMap.getOrDefault(skip.getDate(), 0.0));
            analysis.put("satisfaction_score", 0.0);

            dailyAnalysesList.add(analysis);
        }

        // Process Dinner Skips -> Daily Analysis
        for (SkipMeal skip : dinnerSkips) {
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("date", skip.getDate().toString());
            analysis.put("menu_name", "Dinner Menu");
            analysis.put("meal_type", "DINNER");
            int served = skip.getTotalStudents() - skip.getSkippedCount();
            analysis.put("served_count", served);
            analysis.put("leftover_amount", dinnerLeftoverMap.getOrDefault(skip.getDate(), 0.0));
            analysis.put("satisfaction_score", 0.0);

            dailyAnalysesList.add(analysis);
        }

        payload.put("daily_analyses", dailyAnalysesList);

        // Daily Data (daily_info) - Compatibility
        List<Map<String, Object>> dailyInfoList = new ArrayList<>();

        for (SkipMeal skip : lunchSkips) {
            Map<String, Object> dailyInfo = new HashMap<>();
            dailyInfo.put("date", skip.getDate().toString());
            dailyInfo.put("meal_type", "Lunch");
            dailyInfo.put("served_proxy", skip.getTotalStudents() - skip.getSkippedCount());
            dailyInfo.put("missed_proxy", skip.getSkippedCount());
            dailyInfo.put("leftover_kg", lunchLeftoverMap.getOrDefault(skip.getDate(), 0.0));
            dailyInfoList.add(dailyInfo);
        }

        for (SkipMeal skip : dinnerSkips) {
            Map<String, Object> dailyInfo = new HashMap<>();
            dailyInfo.put("date", skip.getDate().toString());
            dailyInfo.put("meal_type", "Dinner");
            dailyInfo.put("served_proxy", skip.getTotalStudents() - skip.getSkippedCount());
            dailyInfo.put("missed_proxy", skip.getSkippedCount());
            dailyInfo.put("leftover_kg", dinnerLeftoverMap.getOrDefault(skip.getDate(), 0.0));
            dailyInfoList.add(dailyInfo);
        }
        payload.put("daily_info", dailyInfoList);

        // Reviews List
        // [Fix] Stream mapping type inference explicit
        List<Map<String, Object>> reviewList = reviews.stream()
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("content", r.getContent());
                    m.put("rating", r.getRating());
                    // Null safe check for createdAt
                    m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
                    return m;
                })
                .collect(Collectors.toList());
        payload.put("reviews", reviewList);

        // Empty Arrays
        payload.put("meal_plan", new ArrayList<>());
        payload.put("posts", new ArrayList<>());

        return payload;
    }

    // =========================================================================
    // 2. List Retrieval
    // =========================================================================
    public MonthlyOpsDocDto.ListResponse getMonthlyOpsDocList(
            Long schoolId, Integer year, Integer month, int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());

        Page<MonthlyOpsDoc> pageResult = monthlyOpsDocRepository.findAllBySchoolId(schoolId, pageable);

        List<MonthlyOpsDocDto.Response> docList = pageResult.getContent().stream()
                .map(doc -> mapToResponse(doc, null))
                .collect(Collectors.toList());

        MonthlyOpsDocDto.Pagination pagination = MonthlyOpsDocDto.Pagination.builder()
                .current_page(page)
                .total_pages(pageResult.getTotalPages())
                .total_items(pageResult.getTotalElements())
                .page_size(size)
                .build();

        return MonthlyOpsDocDto.ListResponse.builder()
                .reports(docList)
                .pagination(pagination)
                .build();
    }

    // =========================================================================
    // 3. Detail Retrieval
    // =========================================================================
    public MonthlyOpsDocDto.Response getMonthlyOpsDocDetail(Long id) {
        MonthlyOpsDoc doc = monthlyOpsDocRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operation document not found."));

        List<FileAttachment> attachments = fileAttachmentRepository
                .findAllByRelatedTypeAndRelatedId("OPS", id);

        List<MonthlyOpsDocDto.FileResponse> files = attachments.stream()
                .map(file -> MonthlyOpsDocDto.FileResponse.builder()
                        .id(file.getId())
                        .file_name(file.getFileName())
                        .file_type(file.getFileType())
                        .s3_path(file.getS3Path())
                        .created_at(file.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return mapToResponse(doc, files);
    }

    private MonthlyOpsDocDto.Response mapToResponse(
            MonthlyOpsDoc entity,
            List<MonthlyOpsDocDto.FileResponse> files) {

        Map<String, Object> contentMap = null;
        try {
            if (entity.getReportData() != null) {
                contentMap = objectMapper.readValue(entity.getReportData(), Map.class);
            }
        } catch (Exception e) {
            log.error("JSON Parsing Failed ID: {}", entity.getId(), e);
        }

        return MonthlyOpsDocDto.Response.builder()
                .id(entity.getId())
                .school_id(entity.getSchoolId())
                .title(entity.getTitle())
                .year(entity.getYear())
                .month(entity.getMonth())
                .status(entity.getStatus().toString())
                .report_content(contentMap)
                .created_at(entity.getCreatedAt())
                .files(files != null ? files : Collections.emptyList())
                .build();
    }

    // =========================================================================
    // 4. Helper Methods
    // =========================================================================
    public Optional<MonthlyOpsDoc> findByYearAndMonth(int year, int month) {
        return monthlyOpsDocRepository.findByYearAndMonth(year, month);
    }

    public JsonNode getReportDataAsJson(MonthlyOpsDoc doc) {
        try {
            String jsonStr = doc.getReportData();
            if (jsonStr == null || jsonStr.isEmpty()) return null;
            return objectMapper.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            log.error("❌ JSON Parsing Failed (ID: {}): {}", doc.getId(), e.getMessage());
            return null;
        }
    }
}