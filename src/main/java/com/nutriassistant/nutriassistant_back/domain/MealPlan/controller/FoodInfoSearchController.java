package com.nutriassistant.nutriassistant_back.domain.MealPlan.controller;

import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.FoodInfoListResponse;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.service.FoodInfoService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/foodinfo")
public class FoodInfoSearchController {

    private final FoodInfoService foodInfoService;

    public FoodInfoSearchController(FoodInfoService foodInfoService) {
        this.foodInfoService = foodInfoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FoodInfoListResponse>> getFoodInfoList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            // 유효성 검사
            if (size > 100) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("요청 파라미터가 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("size", "max 100"))
                );
            }

            if (page < 1) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("요청 파라미터가 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("page", "min 1"))
                );
            }

            log.info("🔍 메뉴 목록 조회: page={}, size={}", page, size);

            // page는 1부터 시작하지만, Spring Data는 0부터 시작
            FoodInfoListResponse response = foodInfoService.getFoodInfoList(page - 1, size);

            return ResponseEntity.ok(
                    ApiResponse.success("메뉴 목록 조회 성공", response)
            );

        } catch (Exception e) {
            log.error("❌ 메뉴 목록 조회 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId))
            );
        }
    }
}
