package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.controller;

import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.dto.MonthlyOpsDocDto;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.service.MonthlyOpsDocService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monthly-ops")
@RequiredArgsConstructor
public class MonthlyOpsDocController {

    private final MonthlyOpsDocService monthlyOpsDocService;

    // 1. 운영 자료 등록 (생성 요청 Trigger)
    @PostMapping
    public ApiResponse<MonthlyOpsDocDto.Response> createMonthlyOpsDoc(@RequestBody MonthlyOpsDocDto.CreateRequest request) {
        // [수정] 이제 request에는 파일이나 내용이 없고, "생성해줘"라는 명령 정보(학교, 날짜)만 있습니다.
        MonthlyOpsDocDto.Response response = monthlyOpsDocService.createMonthlyOpsDoc(request);
        return ApiResponse.success("월간 운영 자료가 생성되었습니다.", response);
    }

    // 2. 운영 자료 목록 조회
    @GetMapping
    public ApiResponse<MonthlyOpsDocDto.ListResponse> getMonthlyOpsDocList(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        MonthlyOpsDocDto.ListResponse response = monthlyOpsDocService.getMonthlyOpsDocList(schoolId, year, month, page, size);
        return ApiResponse.success("운영 자료 목록 조회 성공", response);
    }

    // 3. 운영 자료 상세 조회
    @GetMapping("/{id}")
    public ApiResponse<MonthlyOpsDocDto.Response> getMonthlyOpsDocDetail(@PathVariable Long id) {
        MonthlyOpsDocDto.Response response = monthlyOpsDocService.getMonthlyOpsDocDetail(id);
        return ApiResponse.success("운영 자료 상세 조회 성공", response);
    }
}