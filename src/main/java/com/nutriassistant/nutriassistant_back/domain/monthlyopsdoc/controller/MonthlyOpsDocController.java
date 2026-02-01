package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.controller;

import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.dto.MonthlyOpsDocDto;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.service.MonthlyOpsDocService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ API 명세서 기준 경로: /reports/monthly
 */
@RestController
@RequestMapping("/reports/monthly")  // ✅ 명세서 경로로 수정
@RequiredArgsConstructor
public class MonthlyOpsDocController {

    private final MonthlyOpsDocService monthlyOpsDocService;

    /**
     * 1. 월간 운영 자료 생성
     * POST /reports/monthly
     */
    @PostMapping
    public ApiResponse<MonthlyOpsDocDto.Response> createMonthlyOpsDoc(
            @RequestBody MonthlyOpsDocDto.CreateRequest request) {

        MonthlyOpsDocDto.Response response = monthlyOpsDocService.createMonthlyOpsDoc(request);
        return ApiResponse.success("월간 운영 자료 생성이 시작되었습니다.", response);
    }

    /**
     * 2. 월간 운영 자료 목록 조회
     * GET /reports/monthly
     */
    @GetMapping
    public ApiResponse<MonthlyOpsDocDto.ListResponse> getMonthlyOpsDocList(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        MonthlyOpsDocDto.ListResponse response =
                monthlyOpsDocService.getMonthlyOpsDocList(schoolId, year, month, page, size);

        return ApiResponse.success("운영 자료 목록 조회 성공", response);
    }

    /**
     * 3. 월간 운영 자료 상세 조회
     * GET /reports/monthly/{reportId}
     */
    @GetMapping("/{reportId}")
    public ApiResponse<MonthlyOpsDocDto.Response> getMonthlyOpsDocDetail(
            @PathVariable Long reportId) {

        MonthlyOpsDocDto.Response response =
                monthlyOpsDocService.getMonthlyOpsDocDetail(reportId);

        return ApiResponse.success("운영 자료 상세 조회 성공", response);
    }

    /**
     * 4. 월간 운영 자료 다운로드
     * GET /reports/monthly/{reportId}/download
     *
     * TODO: 파일 다운로드 구현 필요 (S3 연동 후)
     */
    @GetMapping("/{reportId}/download")
    public ApiResponse<String> downloadMonthlyOpsDoc(
            @PathVariable Long reportId,
            @RequestParam(required = false) Long file_id,
            @RequestParam(defaultValue = "PDF") String format) {

        // TODO: S3에서 파일 다운로드 구현
        return ApiResponse.error("파일 다운로드 기능은 구현 예정입니다.");
    }
}