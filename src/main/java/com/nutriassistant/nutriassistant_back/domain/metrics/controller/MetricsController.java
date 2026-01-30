package com.nutriassistant.nutriassistant_back.domain.metrics.controller;

import com.nutriassistant.nutriassistant_back.domain.metrics.dto.LeftoverDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SatisfactionDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SkipMealDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.service.MetricsService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    // =================================================================================
    // 1. 결식률 (Skip Meal) API
    // =================================================================================

    // [등록] 매일 결식 인원 입력
    @PostMapping("/skip-meal/daily")
    public ApiResponse<SkipMealDto.Response> registerSkipMeal(@RequestBody SkipMealDto.RegisterRequest request) {
        SkipMealDto.Response response = metricsService.registerSkipMeal(request);
        return ApiResponse.success("결식 인원 수가 등록되었습니다.", response);
    }

    // [수정] 오기재 수정
    @PutMapping("/skip-meal/daily")
    public ApiResponse<SkipMealDto.Response> updateSkipMeal(@RequestBody SkipMealDto.UpdateRequest request) {
        SkipMealDto.Response response = metricsService.updateSkipMeal(request);
        return ApiResponse.success("결식 인원 수가 수정되었습니다.", response);
    }

    // [추가] 어제 결식률 조회 (단건)
    @GetMapping("/skip-meal-rate/yesterday")
    public ApiResponse<SkipMealDto.Response> getSkipRateYesterday(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {

        // 어제 날짜 계산
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // [수정] 서비스의 단건 조회 메서드 호출
        SkipMealDto.Response response = metricsService.getDailySkipMeal(schoolId, mealType, yesterday);
        return ApiResponse.success("어제 결식률 조회 성공", response);
    }

    // [추가] 최근 7일 결식률 추이 조회
    @GetMapping("/skip-meal-rate/last-7days")
    public ApiResponse<SkipMealDto.PeriodResponse> getSkipRateLast7Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6); // 오늘 포함 7일

        // [수정] 서비스의 기간 조회 메서드 호출
        SkipMealDto.PeriodResponse response = metricsService.getSkipMealStats(schoolId, mealType, start, end);
        return ApiResponse.success("최근 7일 결식률 조회 성공", response);
    }

    // [추가] 최근 30일 결식률 추이 조회
    @GetMapping("/skip-meal-rate/last-30days")
    public ApiResponse<SkipMealDto.PeriodResponse> getSkipRateLast30Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);

        SkipMealDto.PeriodResponse response = metricsService.getSkipMealStats(schoolId, mealType, start, end);
        return ApiResponse.success("최근 30일 결식률 조회 성공", response);
    }


    // =================================================================================
    // 2. 잔반률 (Leftover) API
    // =================================================================================

    // [등록] 매일 잔반량 입력
    @PostMapping("/leftover/daily")
    public ApiResponse<LeftoverDto.Response> registerLeftover(@RequestBody LeftoverDto.RegisterRequest request) {
        LeftoverDto.Response response = metricsService.registerLeftover(request);
        return ApiResponse.success("잔반량이 등록되었습니다.", response);
    }

    // [수정] 잔반량 수정
    @PutMapping("/leftover/daily")
    public ApiResponse<LeftoverDto.Response> updateLeftover(@RequestBody LeftoverDto.UpdateRequest request) {
        LeftoverDto.Response response = metricsService.updateLeftover(request);
        return ApiResponse.success("잔반량이 수정되었습니다.", response);
    }

    // [추가] 어제 잔반량 조회 (단건)
    @GetMapping("/leftover-rate/yesterday")
    public ApiResponse<LeftoverDto.Response> getLeftoverRateYesterday(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LeftoverDto.Response response = metricsService.getDailyLeftover(schoolId, mealType, yesterday);
        return ApiResponse.success("어제 잔반량 조회 성공", response);
    }

    // [추가] 최근 7일 잔반량 추이 조회
    @GetMapping("/leftover-rate/last-7days")
    public ApiResponse<LeftoverDto.PeriodResponse> getLeftoverRateLast7Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        LeftoverDto.PeriodResponse response = metricsService.getLeftoverStats(schoolId, mealType, start, end);
        return ApiResponse.success("최근 7일 잔반량 조회 성공", response);
    }

    // [추가] 최근 30일 잔반량 추이 조회
    @GetMapping("/leftover-rate/last-30days")
    public ApiResponse<LeftoverDto.PeriodResponse> getLeftoverRateLast30Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        LeftoverDto.PeriodResponse response = metricsService.getLeftoverStats(schoolId, mealType, start, end);
        return ApiResponse.success("최근 30일 잔반량 조회 성공", response);
    }


    // =================================================================================
    // 3. 만족도 (Satisfaction) API - 조회 Only
    // =================================================================================

    // [추가] 최근 30일 만족도 통계 (긍정/부정/중립 개수)
    @GetMapping("/satisfaction/count/last-30days")
    public ApiResponse<SatisfactionDto.CountResponse> getSatisfactionCountLast30Days(
            @RequestParam("school_id") Long schoolId) {

        // 30일 전 ~ 현재까지 조회
        SatisfactionDto.CountResponse response = metricsService.getSatisfactionCount(schoolId, 30);
        return ApiResponse.success("최근 30일 만족도 통계 조회 성공", response);
    }
}