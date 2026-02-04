package com.nutriassistant.nutriassistant_back.domain.MealPlan.controller;

import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.MenuCostDatabaseResponse;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.MenuCostResponse;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.MenuCostUploadRequest;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.service.MenuCostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/costs")
public class MenuCostController {

    private final MenuCostService menuCostService;

    public MenuCostController(MenuCostService menuCostService) {
        this.menuCostService = menuCostService;
    }

    /**
     * 단가 DB 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = menuCostService.getStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * JSON 파일 업로드 (수동)
     */
    @PostMapping("/upload")
    public ResponseEntity<MenuCostDatabaseResponse> uploadJson(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            MenuCostDatabaseResponse response = menuCostService.uploadFromJson(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("JSON 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 메뉴 단가 조회
     */
    @GetMapping("/{menuName}")
    public ResponseEntity<MenuCostResponse> getCost(@PathVariable String menuName) {
        MenuCostResponse response = menuCostService.getCost(menuName);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 단가 DB 조회 (인증 필요)
     */
    @GetMapping
    public ResponseEntity<MenuCostDatabaseResponse> getAllCosts() {
        MenuCostDatabaseResponse response = menuCostService.getAllCosts();
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 단가 DB 조회 (FastAPI 내부 연동용 - API 키 인증)
     */
    @GetMapping("/internal/all")
    public ResponseEntity<MenuCostDatabaseResponse> getAllCostsInternal() {
        MenuCostDatabaseResponse response = menuCostService.getAllCosts();
        return ResponseEntity.ok(response);
    }

    /**
     * 단가 일괄 업데이트 (FastAPI AI 생성용)
     */
    @PostMapping("/bulk")
    public ResponseEntity<String> bulkUpdate(@RequestBody MenuCostUploadRequest request) {
        try {
            int count = menuCostService.bulkUpdate(request);
            return ResponseEntity.ok(count + "개 메뉴 단가 업데이트 완료");
        } catch (Exception e) {
            throw new RuntimeException("일괄 업데이트 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 연도별 재계산
     */
    @PostMapping("/recalculate")
    public ResponseEntity<String> recalculate(@RequestParam Integer year) {
        int count = menuCostService.recalculateForNewYear(year);
        return ResponseEntity.ok(year + "년 기준으로 " + count + "개 재계산 완료");
    }
}