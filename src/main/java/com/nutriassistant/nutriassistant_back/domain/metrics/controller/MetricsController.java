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
@RequestMapping("/metrics") // 이 컨트롤러는 '/metrics'로 시작하는 모든 주소를 담당합니다.
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    // =================================================================================
    // 1. 결식률 (Skip Meal) API
    // =================================================================================

    @PostMapping("/skip-meal/daily")
    public ApiResponse<SkipMealDto.Response> registerSkipMeal(@RequestBody SkipMealDto.RegisterRequest request) {
        SkipMealDto.Response response = metricsService.registerSkipMeal(request);
        return ApiResponse.success("결식 인원 수가 등록되었습니다.", response);
    }

    @PutMapping("/skip-meal/daily")
    public ApiResponse<SkipMealDto.Response> updateSkipMeal(@RequestBody SkipMealDto.UpdateRequest request) {
        SkipMealDto.Response response = metricsService.updateSkipMeal(request);
        return ApiResponse.success("결식 인원 수가 수정되었습니다.", response);
    }

    @GetMapping("/skip-meal-rate/yesterday")
    public ApiResponse<SkipMealDto.Response> getSkipRateYesterday(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SkipMealDto.Response response = metricsService.getDailySkipMeal(schoolId, mealType, yesterday);
        return ApiResponse.success("어제 결식률 조회 성공", response);
    }

    @GetMapping("/skip-meal-rate/last-7days")
    public ApiResponse<SkipMealDto.PeriodResponse> getSkipRateLast7Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(6);
        SkipMealDto.PeriodResponse response = metricsService.getSkipMealStats(schoolId, mealType, start, end);
        return ApiResponse.success("최근 7일 결식률 조회 성공", response);
    }

    @GetMapping("/skip-meal-rate/last-30days")
    public ApiResponse<SkipMealDto.PeriodResponse> getSkipRateLast30Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(29);
        SkipMealDto.PeriodResponse response = metricsService.getSkipMealStats(schoolId, mealType, start, end);
        return ApiResponse.success("최근 30일 결식률 조회 성공", response);
    }


    // =================================================================================
    // 2. 잔반률 (Leftover) API
    // =================================================================================

    @PostMapping("/leftover/daily")
    public ApiResponse<LeftoverDto.Response> registerLeftover(@RequestBody LeftoverDto.RegisterRequest request) {
        LeftoverDto.Response response = metricsService.registerLeftover(request);
        return ApiResponse.success("잔반량이 등록되었습니다.", response);
    }

    @PutMapping("/leftover/daily")
    public ApiResponse<LeftoverDto.Response> updateLeftover(@RequestBody LeftoverDto.UpdateRequest request) {
        LeftoverDto.Response response = metricsService.updateLeftover(request);
        return ApiResponse.success("잔반량이 수정되었습니다.", response);
    }

    @GetMapping("/leftover-rate/yesterday")
    public ApiResponse<LeftoverDto.Response> getLeftoverRateYesterday(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LeftoverDto.Response response = metricsService.getDailyLeftover(schoolId, mealType, yesterday);
        return ApiResponse.success("어제 잔반량 조회 성공", response);
    }

    @GetMapping("/leftover-rate/last-7days")
    public ApiResponse<LeftoverDto.PeriodResponse> getLeftoverRateLast7Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(6);
        LeftoverDto.PeriodResponse response = metricsService.getLeftoverStats(schoolId, mealType, start, end);
        return ApiResponse.success("최근 7일 잔반량 조회 성공", response);
    }

    @GetMapping("/leftover-rate/last-30days")
    public ApiResponse<LeftoverDto.PeriodResponse> getLeftoverRateLast30Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(29);
        LeftoverDto.PeriodResponse response = metricsService.getLeftoverStats(schoolId, mealType, start, end);
        return ApiResponse.success("최근 30일 잔반량 조회 성공", response);
    }


    // =================================================================================
    // 3. 만족도 (Satisfaction) API - [여기가 없어서 404가 났던 것입니다!]
    // =================================================================================

    // 1. 최근 30일 만족도 건수 조회
    // URL: /metrics/satisfaction/count/last-30days
    @GetMapping("/satisfaction/count/last-30days")
    public ApiResponse<SatisfactionDto.CountResponse> getSatisfactionCountLast30Days(
            @RequestParam("school_id") Long schoolId) {
        return ApiResponse.success("성공", metricsService.getSatisfactionCount(schoolId, 30));
    }

    // 2. 최근 30일 만족도 목록(배치) 조회
    // URL: /metrics/satisfaction/last-30days
    @GetMapping("/satisfaction/last-30days")
    public ApiResponse<SatisfactionDto.BatchListResponse> getSatisfactionListLast30Days(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success("성공", metricsService.getSatisfactionBatchList(schoolId, 30, page, size));
    }

    // 3. 긍정 만족도 건수 조회
    // URL: /metrics/satisfaction/positive/count
    @GetMapping("/satisfaction/positive/count")
    public ApiResponse<SatisfactionDto.LabelCountResponse> getPositiveCount(
            @RequestParam("school_id") Long schoolId,
            @RequestParam("start_date") LocalDate startDate,
            @RequestParam("end_date") LocalDate endDate) {
        return ApiResponse.success("성공", metricsService.getSentimentCount(schoolId, "POSITIVE", startDate, endDate));
    }

    // 4. 부정 만족도 건수 조회
    // URL: /metrics/satisfaction/negative/count
    @GetMapping("/satisfaction/negative/count")
    public ApiResponse<SatisfactionDto.LabelCountResponse> getNegativeCount(
            @RequestParam("school_id") Long schoolId,
            @RequestParam("start_date") LocalDate startDate,
            @RequestParam("end_date") LocalDate endDate) {
        return ApiResponse.success("성공", metricsService.getSentimentCount(schoolId, "NEGATIVE", startDate, endDate));
    }

    // 5. 만족도 리뷰 내용 조회
    // URL: /metrics/satisfaction/reviews
    @GetMapping("/satisfaction/reviews")
    public ApiResponse<SatisfactionDto.ReviewListResponse> getReviews(
            @RequestParam("school_id") Long schoolId,
            @RequestParam(value = "batch_id", required = false) String batchId,
            @RequestParam(value = "start_date", required = false) LocalDate startDate,
            @RequestParam(value = "end_date", required = false) LocalDate endDate,
            @RequestParam(value = "sentiment", required = false) String sentiment,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success("성공", metricsService.getSatisfactionReviews(schoolId, batchId, startDate, endDate, sentiment, page, size));
    }
}