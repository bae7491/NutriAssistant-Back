package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.service;

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

// [이미지 확인 완료] 리뷰 관련 패키지 경로 반영
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
import java.time.LocalDateTime; // [추가] 시간 범위 조회를 위해 필요
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

    // [추가] 리뷰 데이터를 가져오기 위한 Repository 주입
    private final ReviewRepository reviewRepository;

    // 1. 운영 자료 생성 (통계 조회 -> AI 분석 -> DB 저장)
    @Transactional
    public MonthlyOpsDocDto.Response createMonthlyOpsDoc(MonthlyOpsDocDto.CreateRequest request) {

        // 1-1. 중복 생성 방지
        if (monthlyOpsDocRepository.existsBySchoolIdAndYearAndMonth(
                request.getSchool_id(), request.getYear(), request.getMonth())) {
            throw new IllegalArgumentException("해당 년월의 운영 자료가 이미 존재합니다.");
        }

        // 1-2. 날짜 범위 계산
        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // [추가] 리뷰 조회용 LocalDateTime 변환 (해당 월 1일 00:00:00 ~ 말일 23:59:59)
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // 1-3. DB에서 통계 데이터 조회
        log.info("📊 통계 데이터 조회 시작: {}년 {}월", request.getYear(), request.getMonth());

        // 중식 데이터
        List<SkipMeal> lunchSkips = skipMealRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "LUNCH", startDate, endDate);
        List<Leftover> lunchLeftovers = leftoverRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "LUNCH", startDate, endDate);

        // 석식 데이터
        List<SkipMeal> dinnerSkips = skipMealRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "DINNER", startDate, endDate);
        List<Leftover> dinnerLeftovers = leftoverRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "DINNER", startDate, endDate);

        // [추가] 해당 기간의 모든 리뷰 데이터 조회
        // ⚠️ 주의: ReviewRepository에 findAllBySchoolIdAndCreatedAtBetween 메서드가 구현되어 있어야 합니다.
        List<Review> monthlyReviews = reviewRepository.findAllBySchoolIdAndCreatedAtBetween(
                request.getSchool_id(), startDateTime, endDateTime);

        log.info("   중식 결식: {}건, 잔반: {}건", lunchSkips.size(), lunchLeftovers.size());
        log.info("   석식 결식: {}건, 잔반: {}건", dinnerSkips.size(), dinnerLeftovers.size());
        log.info("   수집된 리뷰: {}건", monthlyReviews.size()); // [추가] 로그 확인

        // 1-4. FastAPI 요청 페이로드 구성
        Map<String, Object> fastApiPayload = buildFastApiPayload(
                request,
                lunchSkips, lunchLeftovers,
                dinnerSkips, dinnerLeftovers,
                monthlyReviews // [수정] 리뷰 데이터 전달
        );

        // 1-5. FastAPI 호출
        Map<String, Object> analyzedResult;
        try {
            log.info("🤖 FastAPI 분석 요청 시작");

            analyzedResult = restClient.post()
                    .uri("/reports/monthly")
                    .body(fastApiPayload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            log.info("✅ AI 분석 완료");

        } catch (Exception e) {
            log.error("❌ FastAPI 분석 요청 실패", e);
            throw new RuntimeException("AI 분석 서버 오류: " + e.getMessage());
        }

        // 1-6. 결과 저장
        String reportContentJson;
        try {
            Object dataObj = analyzedResult.get("data");
            reportContentJson = objectMapper.writeValueAsString(dataObj);
        } catch (Exception e) {
            log.error("❌ JSON 변환 실패", e);
            throw new RuntimeException("JSON 변환 오류: " + e.getMessage());
        }

        // 1-7. DB 저장
        MonthlyOpsDoc doc = MonthlyOpsDoc.builder()
                .schoolId(request.getSchool_id())
                .title(request.getTitle())
                .year(request.getYear())
                .month(request.getMonth())
                .status(ReportStatus.COMPLETED)
                .reportContent(reportContentJson)
                .build();

        MonthlyOpsDoc savedDoc = monthlyOpsDocRepository.save(doc);
        log.info("💾 리포트 DB 저장 완료: ID={}", savedDoc.getId());

        // 1-8. 응답 반환
        return getMonthlyOpsDocDetail(savedDoc.getId());
    }

    /**
     * FastAPI 요청 페이로드 구성
     */
    private Map<String, Object> buildFastApiPayload(
            MonthlyOpsDocDto.CreateRequest request,
            List<SkipMeal> lunchSkips, List<Leftover> lunchLeftovers,
            List<SkipMeal> dinnerSkips, List<Leftover> dinnerLeftovers,
            List<Review> reviews) { // [수정] 파라미터 추가

        Map<String, Object> payload = new HashMap<>();

        // 기본 정보
        payload.put("userName", "관리자");
        payload.put("year", request.getYear());
        payload.put("month", request.getMonth());
        payload.put("targetGroup", "STUDENT");

        // dailyInfo 구성 (결식 + 잔반 데이터)
        List<Map<String, Object>> dailyInfoList = new ArrayList<>();

        // 중식
        for (int i = 0; i < lunchSkips.size(); i++) {
            SkipMeal skip = lunchSkips.get(i);
            Leftover leftover = i < lunchLeftovers.size() ? lunchLeftovers.get(i) : null;
            Map<String, Object> dailyInfo = new HashMap<>();
            dailyInfo.put("date", skip.getDate().toString());
            dailyInfo.put("mealType", "중식");
            dailyInfo.put("servedProxy", skip.getTotalStudents() - skip.getSkippedCount());
            dailyInfo.put("missedProxy", skip.getSkippedCount());
            dailyInfo.put("leftoverKg", leftover != null ? leftover.getAmountKg() : 0.0);
            dailyInfoList.add(dailyInfo);
        }

        // 석식
        for (int i = 0; i < dinnerSkips.size(); i++) {
            SkipMeal skip = dinnerSkips.get(i);
            Leftover leftover = i < dinnerLeftovers.size() ? dinnerLeftovers.get(i) : null;
            Map<String, Object> dailyInfo = new HashMap<>();
            dailyInfo.put("date", skip.getDate().toString());
            dailyInfo.put("mealType", "석식");
            dailyInfo.put("servedProxy", skip.getTotalStudents() - skip.getSkippedCount());
            dailyInfo.put("missedProxy", skip.getSkippedCount());
            dailyInfo.put("leftoverKg", leftover != null ? leftover.getAmountKg() : 0.0);
            dailyInfoList.add(dailyInfo);
        }

        payload.put("dailyInfo", dailyInfoList);

        // [추가] 리뷰 데이터를 FastAPI가 이해할 수 있는 Map 리스트로 변환
        List<Map<String, Object>> reviewList = reviews.stream()
                .map(review -> {
                    Map<String, Object> map = new HashMap<>();
                    // ⚠️ Review 엔티티 필드 확인 필요 (예: getComment, getScore 등)
                    map.put("content", review.getContent());     // ✅ getContent() 사용
                    map.put("rating", review.getRating());       // ✅ getRating() 사용
                    map.put("createdAt", review.getCreatedAt().toString());

                    return map;
                })
                .collect(Collectors.toList());

        payload.put("reviews", reviewList); // [수정] 빈 리스트 대신 실제 리뷰 데이터 삽입

        // 나머지는 빈 배열로 초기화 (식단표, 게시글 등은 나중에 필요하면 추가)
        payload.put("mealPlan", new ArrayList<>());
        payload.put("posts", new ArrayList<>());

        // 분석 결과가 들어올 빈 공간들
        payload.put("reviewAnalyses", new ArrayList<>());
        payload.put("postAnalyses", new ArrayList<>());
        payload.put("dailyAnalyses", new ArrayList<>());

        return payload;
    }

    // 2. 목록 조회
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

    // 3. 상세 조회
    public MonthlyOpsDocDto.Response getMonthlyOpsDocDetail(Long id) {
        MonthlyOpsDoc doc = monthlyOpsDocRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 운영 자료를 찾을 수 없습니다."));

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

    // 매핑 헬퍼
    private MonthlyOpsDocDto.Response mapToResponse(
            MonthlyOpsDoc entity,
            List<MonthlyOpsDocDto.FileResponse> files) {

        Map<String, Object> contentMap = null;
        try {
            if (entity.getReportContent() != null) {
                contentMap = objectMapper.readValue(entity.getReportContent(), Map.class);
            }
        } catch (Exception e) {
            log.error("JSON 파싱 실패 ID: {}", entity.getId(), e);
        }

        List<MonthlyOpsDocDto.FileResponse> safeFiles =
                (files != null) ? files : Collections.emptyList();

        return MonthlyOpsDocDto.Response.builder()
                .id(entity.getId())
                .school_id(entity.getSchoolId())
                .title(entity.getTitle())
                .year(entity.getYear())
                .month(entity.getMonth())
                .status(entity.getStatus().toString())
                .report_content(contentMap)
                .created_at(entity.getCreatedAt())
                .files(safeFiles)
                .build();
    }
}