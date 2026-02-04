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
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.MealPlanMenuRepository;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.repository.ReviewAnalysisRepository;

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
    private final MealPlanMenuRepository mealPlanMenuRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;

    // =========================================================================
    // 1. [Create] Create Operation Document (Stats -> AI Analysis -> DB Save)
    // =========================================================================
    @Transactional
    public MonthlyOpsDocDto.Response createMonthlyOpsDoc(MonthlyOpsDocDto.CreateRequest request, Long schoolId) {

        // 1-1. Prevent Duplicate Creation
        if (monthlyOpsDocRepository.existsBySchoolIdAndYearAndMonth(
                schoolId, request.getYear(), request.getMonth())) {
            throw new IllegalArgumentException("An operation document for this month already exists.");
        }

        // 1-2. Calculate Date Range
        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // 1-3. Fetch Statistics Data from DB
        log.info("📊 Fetching statistics data: {}-{}, schoolId: {}", request.getYear(), request.getMonth(), schoolId);
        log.info("   Date range: {} ~ {}", startDate, endDate);

        List<SkipMeal> lunchSkips = skipMealRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                schoolId, "LUNCH", startDate, endDate);
        List<Leftover> lunchLeftovers = leftoverRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                schoolId, "LUNCH", startDate, endDate);

        List<SkipMeal> dinnerSkips = skipMealRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                schoolId, "DINNER", startDate, endDate);
        List<Leftover> dinnerLeftovers = leftoverRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                schoolId, "DINNER", startDate, endDate);

        List<Review> monthlyReviews = reviewRepository.findBySchoolIdAndCreatedAtBetween(
                schoolId, startDateTime, endDateTime);

        log.info("   Lunch Data: {}, Dinner Data: {}", lunchSkips.size(), dinnerSkips.size());
        log.info("   Collected Reviews: {}", monthlyReviews.size());

        // 1-4. Construct Payload for FastAPI Request
        Map<String, Object> fastApiPayload = buildFastApiPayload(
                request, schoolId,
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
                .schoolId(schoolId)
                .title(request.getTitle())
                .year(request.getYear())
                .month(request.getMonth())
                .status(ReportStatus.COMPLETED)
                .reportData(reportContentJson)
                .build();

        MonthlyOpsDoc savedDoc = monthlyOpsDocRepository.save(doc);
        log.info("💾 Report saved to DB: ID={}", savedDoc.getId());

        return getMonthlyOpsDocDetail(savedDoc.getId(), schoolId);
    }

    /**
     * Constructs the data structure to send to FastAPI.
     * [Fix] Changed to camelCase to match FastAPI schema (MonthlyReportRequestPayload).
     */
    private Map<String, Object> buildFastApiPayload(
            MonthlyOpsDocDto.CreateRequest request, Long schoolId,
            List<SkipMeal> lunchSkips, List<Leftover> lunchLeftovers,
            List<SkipMeal> dinnerSkips, List<Leftover> dinnerLeftovers,
            List<Review> reviews) {

        Map<String, Object> payload = new HashMap<>();

        // Meta Information (camelCase for FastAPI)
        payload.put("userName", "Administrator");
        payload.put("year", request.getYear());
        payload.put("month", request.getMonth());
        payload.put("targetGroup", "STUDENT");
        payload.put("school_id", schoolId);

        // Build leftover maps for lookup
        Map<LocalDate, Double> lunchLeftoverMap = lunchLeftovers.stream()
                .collect(Collectors.toMap(
                        leftover -> leftover.getDate(),
                        leftover -> leftover.getAmountKg() != null ? leftover.getAmountKg() : 0.0,
                        (existing, replacement) -> replacement
                ));

        Map<LocalDate, Double> dinnerLeftoverMap = dinnerLeftovers.stream()
                .collect(Collectors.toMap(
                        leftover -> leftover.getDate(),
                        leftover -> leftover.getAmountKg() != null ? leftover.getAmountKg() : 0.0,
                        (existing, replacement) -> replacement
                ));

        // ========== dailyInfo (camelCase) ==========
        List<Map<String, Object>> dailyInfoList = new ArrayList<>();

        for (SkipMeal skip : lunchSkips) {
            Map<String, Object> dailyInfo = new HashMap<>();
            dailyInfo.put("date", skip.getDate().toString());
            dailyInfo.put("mealType", "중식");
            dailyInfo.put("servedProxy", skip.getTotalStudents() - skip.getSkippedCount());
            dailyInfo.put("missedProxy", skip.getSkippedCount());
            dailyInfo.put("leftoverKg", lunchLeftoverMap.getOrDefault(skip.getDate(), 0.0));
            dailyInfoList.add(dailyInfo);
        }

        for (SkipMeal skip : dinnerSkips) {
            Map<String, Object> dailyInfo = new HashMap<>();
            dailyInfo.put("date", skip.getDate().toString());
            dailyInfo.put("mealType", "석식");
            dailyInfo.put("servedProxy", skip.getTotalStudents() - skip.getSkippedCount());
            dailyInfo.put("missedProxy", skip.getSkippedCount());
            dailyInfo.put("leftoverKg", dinnerLeftoverMap.getOrDefault(skip.getDate(), 0.0));
            dailyInfoList.add(dailyInfo);
        }
        payload.put("dailyInfo", dailyInfoList);

        // ========== dailyAnalyses (camelCase with kpis structure) ==========
        List<Map<String, Object>> dailyAnalysesList = new ArrayList<>();
        int reviewIndex = 0;

        for (SkipMeal skip : lunchSkips) {
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("date", skip.getDate().toString());
            analysis.put("mealType", "중식");

            // kpis object
            Map<String, Object> kpis = new HashMap<>();
            kpis.put("review_count", reviews.size());
            kpis.put("post_count", 0);
            kpis.put("avg_rating_5", reviews.isEmpty() ? 0.0 :
                    reviews.stream().mapToDouble(r -> r.getRating() != null ? r.getRating() : 0.0).average().orElse(0.0));
            kpis.put("avg_review_sentiment", 0.5);
            kpis.put("avg_post_sentiment", 0.0);
            kpis.put("overall_sentiment", 0.5);
            analysis.put("kpis", kpis);

            // distributions object
            Map<String, Object> distributions = new HashMap<>();
            Map<String, Integer> reviewDist = new HashMap<>();
            reviewDist.put("positive", 0);
            reviewDist.put("neutral", reviews.size());
            reviewDist.put("negative", 0);
            distributions.put("reviews", reviewDist);
            distributions.put("posts", new HashMap<>());
            distributions.put("post_categories", new HashMap<>());
            analysis.put("distributions", distributions);

            analysis.put("top_aspects", new ArrayList<>());
            analysis.put("top_negative_aspects", new ArrayList<>());
            analysis.put("top_issues", new ArrayList<>());
            analysis.put("alerts", new ArrayList<>());

            dailyAnalysesList.add(analysis);
        }

        for (SkipMeal skip : dinnerSkips) {
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("date", skip.getDate().toString());
            analysis.put("mealType", "석식");

            Map<String, Object> kpis = new HashMap<>();
            kpis.put("review_count", 0);
            kpis.put("post_count", 0);
            kpis.put("avg_rating_5", 0.0);
            kpis.put("avg_review_sentiment", 0.0);
            kpis.put("avg_post_sentiment", 0.0);
            kpis.put("overall_sentiment", 0.0);
            analysis.put("kpis", kpis);

            Map<String, Object> distributions = new HashMap<>();
            distributions.put("reviews", new HashMap<>());
            distributions.put("posts", new HashMap<>());
            distributions.put("post_categories", new HashMap<>());
            analysis.put("distributions", distributions);

            analysis.put("top_aspects", new ArrayList<>());
            analysis.put("top_negative_aspects", new ArrayList<>());
            analysis.put("top_issues", new ArrayList<>());
            analysis.put("alerts", new ArrayList<>());

            dailyAnalysesList.add(analysis);
        }
        payload.put("dailyAnalyses", dailyAnalysesList);

        // ========== reviews (camelCase) ==========
        List<Map<String, Object>> reviewList = new ArrayList<>();
        int idx = 0;
        for (Review r : reviews) {
            Map<String, Object> m = new HashMap<>();
            String dateStr = r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "";
            m.put("reviewId", String.format("R-%s-%04d", dateStr.replace("-", ""), ++idx));
            m.put("date", dateStr);
            m.put("mealType", "중식");
            m.put("rating", r.getRating() != null ? r.getRating() : 0.0);
            m.put("content", r.getContent() != null ? r.getContent() : "");
            reviewList.add(m);
        }
        payload.put("reviews", reviewList);

        // ========== mealPlan: DB에서 해당 월의 식단 데이터 조회 ==========
        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<MealPlanMenu> mealPlanMenus = mealPlanMenuRepository
                .findByMealPlan_SchoolIdAndMenuDateBetweenOrderByMenuDateAscMealTypeAsc(schoolId, startDate, endDate);

        List<Map<String, Object>> mealPlanList = new ArrayList<>();
        for (MealPlanMenu menu : mealPlanMenus) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", menu.getMenuDate().toString());
            m.put("mealType", menu.getMealType() != null ? menu.getMealType().name() : "LUNCH");
            m.put("rice", menu.getRiceDisplay());
            m.put("soup", menu.getSoupDisplay());
            m.put("main1", menu.getMain1Display());
            m.put("main2", menu.getMain2Display());
            m.put("side", menu.getSideDisplay());
            m.put("kimchi", menu.getKimchiDisplay());
            m.put("dessert", menu.getDessertDisplay());
            mealPlanList.add(m);
        }
        payload.put("mealPlan", mealPlanList);
        log.info("📋 mealPlan 데이터 {}개 로드됨", mealPlanList.size());

        // ========== reviewAnalyses: reviews 기반으로 개별 리뷰 분석 결과 생성 ==========
        // FastAPI는 개별 리뷰에 대한 분석 결과를 기대함 (review_id로 날짜 추출)
        List<Map<String, Object>> reviewAnalysesPayload = new ArrayList<>();
        int raIdx = 0;
        for (Review r : reviews) {
            Map<String, Object> m = new HashMap<>();

            // 날짜 문자열 생성 (R-YYYYMMDD-NNNN 형식)
            String dateStr = r.getCreatedAt() != null
                    ? r.getCreatedAt().toLocalDate().toString().replace("-", "")
                    : String.format("%04d%02d01", request.getYear(), request.getMonth());

            // 필수 필드
            m.put("review_id", String.format("R-%s-%04d", dateStr, ++raIdx));
            m.put("meal_type", r.getMealType() != null ? r.getMealType() : "LUNCH");
            m.put("rating_5", r.getRating() != null ? r.getRating().doubleValue() : 0.0);

            // 감정 분석 결과 (rating 기반으로 추정)
            double rating = r.getRating() != null ? r.getRating().doubleValue() : 3.0;
            String sentimentLabel;
            double sentimentScore;
            if (rating >= 4.0) {
                sentimentLabel = "POSITIVE";
                sentimentScore = 0.7 + (rating - 4.0) * 0.15;  // 4점: 0.7, 5점: 0.85
            } else if (rating >= 3.0) {
                sentimentLabel = "NEUTRAL";
                sentimentScore = 0.4 + (rating - 3.0) * 0.3;  // 3점: 0.4, 4점: 0.7
            } else {
                sentimentLabel = "NEGATIVE";
                sentimentScore = rating / 3.0 * 0.4;  // 1점: 0.13, 2점: 0.27
            }

            m.put("sentiment_label", sentimentLabel);
            m.put("sentiment_score", sentimentScore);
            m.put("sentiment_conf", 0.85);  // 기본 신뢰도

            // 리스트 타입 필드들
            m.put("aspect_tags", new ArrayList<String>());

            List<String> evidencePhrases = new ArrayList<>();
            if (r.getContent() != null && !r.getContent().isBlank()) {
                evidencePhrases.add(r.getContent());
            }
            m.put("evidence_phrases", evidencePhrases);

            m.put("issue_flags", new ArrayList<String>());

            reviewAnalysesPayload.add(m);
        }
        payload.put("reviewAnalyses", reviewAnalysesPayload);
        log.info("📊 reviewAnalyses 데이터 {}개 생성됨 (reviews 기반)", reviewAnalysesPayload.size());

        // ========== posts, postAnalyses: 제외이므로 빈 배열 유지 ==========
        payload.put("posts", new ArrayList<>());
        payload.put("postAnalyses", new ArrayList<>());

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
    public MonthlyOpsDocDto.Response getMonthlyOpsDocDetail(Long id, Long schoolId) {
        MonthlyOpsDoc doc = monthlyOpsDocRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Operation document not found."));

        // schoolId 검증: 본인 학교 문서만 조회 가능
        if (!doc.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Operation document not found.");
        }

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