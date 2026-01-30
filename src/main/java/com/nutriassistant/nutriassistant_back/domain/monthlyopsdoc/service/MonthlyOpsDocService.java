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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyOpsDocService {

    private final MonthlyOpsDocRepository monthlyOpsDocRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final ObjectMapper objectMapper;
//    private final S3Uploader s3Uploader;

    // [수정] WebClient 대신 RestClient 주입
    private final RestClient restClient;

    // [수정] 통계 데이터 조회를 위한 Repository 주입 (주석 해제 완료)
    private final SkipMealRepository skipMealRepository;
    private final LeftoverRepository leftoverRepository;

    // 1. 운영 자료 생성 (통계 조회 -> AI 분석 -> DB 저장 -> S3 파일 생성)
    @Transactional
    public MonthlyOpsDocDto.Response createMonthlyOpsDoc(MonthlyOpsDocDto.CreateRequest request) {

        // 1-1. 중복 생성 방지
        if (monthlyOpsDocRepository.existsBySchoolIdAndYearAndMonth(
                request.getSchool_id(), request.getYear(), request.getMonth())) {
            throw new IllegalArgumentException("해당 년월의 운영 자료가 이미 존재합니다.");
        }

        // 1-2. 날짜 범위 계산 (예: 2026년 1월 1일 ~ 1월 31일)
        YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 1-3. [재료 준비] DB에서 실제 통계 데이터 조회
        // (MetricsService에 구현된 로직을 활용하거나, Repository를 직접 호출)
        // 여기서는 Repository를 통해 해당 기간의 모든 데이터를 가져옵니다.
        List<SkipMeal> skippingStats = skipMealRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "LUNCH", startDate, endDate);
        // 주의: 점심(LUNCH)/저녁(DINNER) 구분이 필요하다면 로직 추가 필요. 현재는 예시로 LUNCH만 조회하거나 전체 조회

        List<Leftover> leftoverStats = leftoverRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                request.getSchool_id(), "LUNCH", startDate, endDate);


        // 1-4. [분석 요청] 통계 데이터를 포함하여 FastAPI로 전송 (RestClient 사용)
        Map<String, Object> analyzedResult;
        try {
            log.info("FastAPI 분석 요청 시작: SchoolID={}, Date={}", request.getSchool_id(), yearMonth);

            analyzedResult = restClient.post()
                    .uri("/analyze/monthly-report") // Config에 BaseURL(localhost:8000) 설정됨
                    .body(Map.of(
                            "school_id", request.getSchool_id(),
                            "year", request.getYear(),
                            "month", request.getMonth(),
                            "skipping_stats", skippingStats, // DB에서 가져온 리스트
                            "leftover_stats", leftoverStats  // DB에서 가져온 리스트
                    ))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}); // Map으로 응답 받기

            log.info("AI 통합 분석 완료");
        } catch (Exception e) {
            log.error("AI 분석 요청 실패", e);
            throw new RuntimeException("분석 서버 오류로 인해 리포트를 생성할 수 없습니다.");
        }

        // 1-5. [결과 변환] Map -> JSON String
        String reportContentJson;
        try {
            reportContentJson = objectMapper.writeValueAsString(analyzedResult);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 오류", e);
        }

        // 1-6. [DB 저장] MonthlyOpsDoc 테이블 (화면 출력용 JSON 저장)
        MonthlyOpsDoc doc = MonthlyOpsDoc.builder()
                .schoolId(request.getSchool_id())
                .title(request.getTitle())
                .year(request.getYear())
                .month(request.getMonth())
                .status(ReportStatus.COMPLETED)
                .reportContent(reportContentJson)
                .build();

        MonthlyOpsDoc savedDoc = monthlyOpsDocRepository.save(doc);

        // 1-7. [S3 업로드] JSON 내용을 파일로 만들어 S3 저장 (다운로드용)
//        try {
//            String s3FileName = String.format("report_%d_%d_%02d.json",
//                    request.getSchool_id(), request.getYear(), request.getMonth());
//
//            String s3Url = s3Uploader.uploadByte(
//                    reportContentJson.getBytes(StandardCharsets.UTF_8),
//                    s3FileName
//            );
//
//            // 1-8. [파일 DB 저장] file_attachments 테이블에 파일 정보 기록
//            FileAttachment jsonFile = FileAttachment.builder()
//                    .relatedType("OPS")
//                    .relatedId(savedDoc.getId())
//                    .fileName(s3FileName)
//                    .s3Path(s3Url)
//                    .fileType("application/json")
//                    .build();
//
//            fileAttachmentRepository.save(jsonFile);
//
//        } catch (Exception e) {
//            log.error("S3 파일 업로드 실패 (DB 저장은 완료됨)", e);
//        }

        // 1-9. 최종 응답 반환
        return getMonthlyOpsDocDetail(savedDoc.getId());
    }

    // 2. 목록 조회
    public MonthlyOpsDocDto.ListResponse getMonthlyOpsDocList(Long schoolId, Integer year, Integer month, int page, int size) {
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

        List<FileAttachment> attachments = fileAttachmentRepository.findAllByRelatedTypeAndRelatedId("OPS", id);

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

    // 매핑 헬퍼 메서드
    private MonthlyOpsDocDto.Response mapToResponse(MonthlyOpsDoc entity, List<MonthlyOpsDocDto.FileResponse> files) {
        Map<String, Object> contentMap = null;
        try {
            if (entity.getReportContent() != null) {
                contentMap = objectMapper.readValue(entity.getReportContent(), Map.class);
            }
        } catch (Exception e) {
            log.error("JSON 파싱 실패 ID: " + entity.getId(), e);
        }

        List<MonthlyOpsDocDto.FileResponse> safeFiles = (files != null) ? files : Collections.emptyList();

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