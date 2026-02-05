package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.controller;

import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.dto.MonthlyOpsDocDto;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.service.MonthlyOpsDocService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import com.nutriassistant.nutriassistant_back.global.auth.CurrentUser;
import com.nutriassistant.nutriassistant_back.global.auth.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @CurrentUser UserContext user,
            @RequestBody MonthlyOpsDocDto.CreateRequest request) {

        MonthlyOpsDocDto.Response response = monthlyOpsDocService.createMonthlyOpsDoc(request, user.getSchoolId());
        return ApiResponse.success("월간 운영 자료 생성이 시작되었습니다.", response);
    }

    /**
     * 2. 월간 운영 자료 목록 조회
     * GET /reports/monthly
     */
    @GetMapping
    public ApiResponse<MonthlyOpsDocDto.ListResponse> getMonthlyOpsDocList(
            @CurrentUser UserContext user,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        MonthlyOpsDocDto.ListResponse response =
                monthlyOpsDocService.getMonthlyOpsDocList(user.getSchoolId(), year, month, page, size);

        return ApiResponse.success("운영 자료 목록 조회 성공", response);
    }

    /**
     * 3. 월간 운영 자료 상세 조회
     * GET /reports/monthly/{reportId}
     */
    @GetMapping("/{reportId}")
    public ApiResponse<MonthlyOpsDocDto.Response> getMonthlyOpsDocDetail(
            @CurrentUser UserContext user,
            @PathVariable Long reportId) {

        MonthlyOpsDocDto.Response response =
                monthlyOpsDocService.getMonthlyOpsDocDetail(reportId, user.getSchoolId());

        return ApiResponse.success("운영 자료 상세 조회 성공", response);
    }

    /**
     * 4. 월간 운영 자료 다운로드
     * GET /reports/monthly/{reportId}/download
     * S3 URL로 리다이렉트하여 파일 다운로드
     */
    @GetMapping("/{reportId}/download")
    public ResponseEntity<Void> downloadMonthlyOpsDoc(
            @CurrentUser UserContext user,
            @PathVariable Long reportId) {

        String downloadUrl = monthlyOpsDocService.getDownloadUrl(reportId, user.getSchoolId());

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, downloadUrl)
                .build();
    }
}