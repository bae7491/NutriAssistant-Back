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

        log.info("   중식 결식: {}건, 잔반: {}건", lunchSkips.size(), lunchLeftovers.size());
        log.info("   석식 결식: {}건, 잔반: {}건", dinnerSkips.size(), dinnerLeftovers.size());

        // 1-4. FastAPI 요청 페이로드 구성
        Map<String, Object> fastApiPayload = buildFastApiPayload(
                request,
                lunchSkips, lunchLeftovers,
                dinnerSkips, dinnerLeftovers
        );

        // 1-5. FastAPI 호출 (수정된 경로)
        Map<String, Object> analyzedResult;
        try {
            log.info("🤖 FastAPI 분석 요청 시작");

            analyzedResult = restClient.post()
                    .uri("/reports/monthly")  // ✅ 올바른 경로
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
            // FastAPI 응답에서 data 부분 추출
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
            List<SkipMeal> dinnerSkips, List<Leftover> dinnerLeftovers) {

        Map<String, Object> payload = new HashMap<>();

        // 기본 정보
        payload.put("userName", "관리자");  // TODO: 실제 사용자명 매핑
        payload.put("year", request.getYear());
        payload.put("month", request.getMonth());
        payload.put("targetGroup", "");  // TODO: 학교 급식 대상 그룹 매핑

        // dailyInfo 구성 (결식 + 잔반 데이터)
        List<Map<String, Object>> dailyInfoList = new ArrayList<>();

        // 중식 데이터 추가
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

        // 석식 데이터 추가
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

        // 빈 배열로 초기화 (리뷰, 게시물 등은 나중에 추가)
        payload.put("mealPlan", new ArrayList<>());
        payload.put("reviews", new ArrayList<>());
        payload.put("posts", new ArrayList<>());
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