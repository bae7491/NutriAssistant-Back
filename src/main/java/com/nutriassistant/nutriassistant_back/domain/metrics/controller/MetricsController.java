package com.nutriassistant.nutriassistant_back.domain.metrics.controller;

import com.nutriassistant.nutriassistant_back.domain.metrics.dto.LeftoverDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SatisfactionDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SkipMealDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.service.MetricsService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import com.nutriassistant.nutriassistant_back.global.auth.CurrentUser;
import com.nutriassistant.nutriassistant_back.global.auth.UserContext;
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

    @PostMapping("/skip-meal/daily")
    public ApiResponse<SkipMealDto.Response> registerSkipMeal(
            @CurrentUser UserContext user,
            @RequestBody SkipMealDto.RegisterRequest request) {
        SkipMealDto.Response response = metricsService.registerSkipMeal(request, user.getSchoolId());
        return ApiResponse.success("결식 인원 수가 등록되었습니다.", response);
    }

    @PutMapping("/skip-meal/daily")
    public ApiResponse<SkipMealDto.Response> updateSkipMeal(
            @CurrentUser UserContext user,
            @RequestBody SkipMealDto.UpdateRequest request) {
        SkipMealDto.Response response = metricsService.updateSkipMeal(request, user.getSchoolId());
        return ApiResponse.success("결식 인원 수가 수정되었습니다.", response);
    }

    @GetMapping("/skip-meal-rate/yesterday")
    public ApiResponse<SkipMealDto.Response> getSkipRateYesterday(
            @CurrentUser UserContext user,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        SkipMealDto.Response response = metricsService.getDailySkipMeal(user.getSchoolId(), mealType, yesterday);
        return ApiResponse.success("어제 결식률 조회 성공", response);
    }

    @GetMapping("/skip-meal-rate/last-7days")
    public ApiResponse<SkipMealDto.PeriodResponse> getSkipRateLast7Days(
            @CurrentUser UserContext user,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(6);
        SkipMealDto.PeriodResponse response = metricsService.getSkipMealStats(user.getSchoolId(), mealType, start, end);
        return ApiResponse.success("최근 7일 결식률 조회 성공", response);
    }

    @GetMapping("/skip-meal-rate/last-30days")
    public ApiResponse<SkipMealDto.PeriodResponse> getSkipRateLast30Days(
            @CurrentUser UserContext user,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(29);
        SkipMealDto.PeriodResponse response = metricsService.getSkipMealStats(user.getSchoolId(), mealType, start, end);
        return ApiResponse.success("최근 30일 결식률 조회 성공", response);
    }

    // [추가] 월간 결식 데이터 조회 (달력용)
    @GetMapping("/skip-meal/monthly")
    public ApiResponse<SkipMealDto.PeriodResponse> getSkipMealMonthly(
            @CurrentUser UserContext user,
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        SkipMealDto.PeriodResponse response = metricsService.getSkipMealStats(user.getSchoolId(), mealType, start, end);
        return ApiResponse.success(year + "년 " + month + "월 결식 데이터 조회 성공", response);
    }


    // =================================================================================
    // 2. 잔반률 (Leftover) API
    // =================================================================================

    @PostMapping("/leftover/daily")
    public ApiResponse<LeftoverDto.Response> registerLeftover(
            @CurrentUser UserContext user,
            @RequestBody LeftoverDto.RegisterRequest request) {
        LeftoverDto.Response response = metricsService.registerLeftover(request, user.getSchoolId());
        return ApiResponse.success("잔반량이 등록되었습니다.", response);
    }

    @PutMapping("/leftover/daily")
    public ApiResponse<LeftoverDto.Response> updateLeftover(
            @CurrentUser UserContext user,
            @RequestBody LeftoverDto.UpdateRequest request) {
        LeftoverDto.Response response = metricsService.updateLeftover(request, user.getSchoolId());
        return ApiResponse.success("잔반량이 수정되었습니다.", response);
    }

    @GetMapping("/leftover-rate/yesterday")
    public ApiResponse<LeftoverDto.Response> getLeftoverRateYesterday(
            @CurrentUser UserContext user,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LeftoverDto.Response response = metricsService.getDailyLeftover(user.getSchoolId(), mealType, yesterday);
        return ApiResponse.success("어제 잔반량 조회 성공", response);
    }

    @GetMapping("/leftover-rate/last-7days")
    public ApiResponse<LeftoverDto.PeriodResponse> getLeftoverRateLast7Days(
            @CurrentUser UserContext user,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(6);
        LeftoverDto.PeriodResponse response = metricsService.getLeftoverStats(user.getSchoolId(), mealType, start, end);
        return ApiResponse.success("최근 7일 잔반량 조회 성공", response);
    }

    @GetMapping("/leftover-rate/last-30days")
    public ApiResponse<LeftoverDto.PeriodResponse> getLeftoverRateLast30Days(
            @CurrentUser UserContext user,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(29);
        LeftoverDto.PeriodResponse response = metricsService.getLeftoverStats(user.getSchoolId(), mealType, start, end);
        return ApiResponse.success("최근 30일 잔반량 조회 성공", response);
    }

    // [추가] 월간 잔반 데이터 조회 (달력용)
    @GetMapping("/leftover/monthly")
    public ApiResponse<LeftoverDto.PeriodResponse> getLeftoverMonthly(
            @CurrentUser UserContext user,
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @RequestParam(value = "meal_type", defaultValue = "LUNCH") String mealType) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        LeftoverDto.PeriodResponse response = metricsService.getLeftoverStats(user.getSchoolId(), mealType, start, end);
        return ApiResponse.success(year + "년 " + month + "월 잔반 데이터 조회 성공", response);
    }


    // =================================================================================
    // 3. 만족도 (Satisfaction) API
    // =================================================================================

    @GetMapping("/satisfaction/count/last-30days")
    public ApiResponse<SatisfactionDto.CountResponse> getSatisfactionCountLast30Days(
            @CurrentUser UserContext user) {
        return ApiResponse.success("성공", metricsService.getSatisfactionCount(user.getSchoolId(), 30));
    }

    @GetMapping("/satisfaction/last-30days")
    public ApiResponse<SatisfactionDto.BatchListResponse> getSatisfactionListLast30Days(
            @CurrentUser UserContext user,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success("성공", metricsService.getSatisfactionBatchList(user.getSchoolId(), 30, page, size));
    }

    @GetMapping("/satisfaction/positive/count")
    public ApiResponse<SatisfactionDto.LabelCountResponse> getPositiveCount(
            @CurrentUser UserContext user,
            @RequestParam("start_date") LocalDate startDate,
            @RequestParam("end_date") LocalDate endDate) {
        return ApiResponse.success("성공", metricsService.getSentimentCount(user.getSchoolId(), "POSITIVE", startDate, endDate));
    }

    @GetMapping("/satisfaction/negative/count")
    public ApiResponse<SatisfactionDto.LabelCountResponse> getNegativeCount(
            @CurrentUser UserContext user,
            @RequestParam("start_date") LocalDate startDate,
            @RequestParam("end_date") LocalDate endDate) {
        return ApiResponse.success("성공", metricsService.getSentimentCount(user.getSchoolId(), "NEGATIVE", startDate, endDate));
    }

    @GetMapping("/satisfaction/reviews")
    public ApiResponse<SatisfactionDto.ReviewListResponse> getReviews(
            @CurrentUser UserContext user,
            @RequestParam(value = "batch_id", required = false) String batchId,
            @RequestParam(value = "start_date", required = false) LocalDate startDate,
            @RequestParam(value = "end_date", required = false) LocalDate endDate,
            @RequestParam(value = "sentiment", required = false) String sentiment,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success("성공", metricsService.getSatisfactionReviews(user.getSchoolId(), batchId, startDate, endDate, sentiment, page, size));
    }
}
