package com.nutriassistant.nutriassistant_back.controller;

import com.nutriassistant.nutriassistant_back.DTO.MealMenuResponse;
import com.nutriassistant.nutriassistant_back.DTO.MealPlanGenerateRequest;
import com.nutriassistant.nutriassistant_back.DTO.MealPlanIdResponse;
import com.nutriassistant.nutriassistant_back.DTO.MealPlanResponse;
import com.nutriassistant.nutriassistant_back.controller.MealPlanController.ManualUpdateRequest; // (이건 기존에 있을 것임)
import com.nutriassistant.nutriassistant_back.entity.MealPlan;
import com.nutriassistant.nutriassistant_back.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.entity.MenuHistory; // [임포트 확인]
import com.nutriassistant.nutriassistant_back.repository.MealPlanMenuRepository;
import com.nutriassistant.nutriassistant_back.repository.MenuHistoryRepository; // [추가]
import com.nutriassistant.nutriassistant_back.service.MealPlanService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/mealplan") // [중요] Postman 주소와 일치시킴 (/mealplan)
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final MealPlanMenuRepository mealPlanMenuRepository;
    private final ObjectMapper objectMapper;
    private final MenuHistoryRepository menuHistoryRepository;

    public MealPlanController(MealPlanService mealPlanService,
                              MealPlanMenuRepository mealPlanMenuRepository,
                              MenuHistoryRepository menuHistoryRepository, // [추가] 주입
                              ObjectMapper objectMapper) {
        this.mealPlanService = mealPlanService;
        this.mealPlanMenuRepository = mealPlanMenuRepository;
        this.menuHistoryRepository = menuHistoryRepository;
        this.objectMapper = objectMapper;
    }

    // 1. [POST] 월간 식단 생성
    // 주소: POST mealplan/generate?year=2026&month=3
    @PostMapping("/generate")
    public ResponseEntity<MealPlanIdResponse> generate(@RequestBody MealPlanGenerateRequest req) {
        MealPlan saved = mealPlanService.generateAndSave(req);
        return ResponseEntity.ok(new MealPlanIdResponse(saved.getId()));
    }

    // 2. [GET] 월간 식단 조회
    // 주소: GET mealplan/{id}
    @GetMapping("/{id}")
    public ResponseEntity<MealPlanResponse> getOne(@PathVariable Long id) {
        MealPlan plan = mealPlanService.getById(id);
        List<MealPlanMenu> menuList = mealPlanMenuRepository.findAllByMealPlanId(id);

        List<MealMenuResponse> menus = menuList.stream()
                .map(this::toMealMenuResponse)
                .toList();

        return ResponseEntity.ok(new MealPlanResponse(
                plan.getId(), plan.getYear(), plan.getMonth(), plan.getGeneratedAt(), menus
        ));
    }

    // 3. [POST] 1끼 AI 자동 대체 (사용자가 찾던 그 기능!)
    // 주소: POST mealplan/ai/replace
    @PostMapping("/ai/replace")
    public ResponseEntity<String> replaceWithAi(@RequestBody Map<String, String> req) {
        // Postman Body 예시: { "date": "2026-03-03", "mealType": "LUNCH" }
        String date = req.get("date");
        String mealType = req.get("mealType");

        mealPlanService.replaceMenuWithAi(date, mealType);
        return ResponseEntity.ok("AI replaced successfully");
    }

    // 4. [POST] 수동 수정
    // 주소: POST mealplan/manual/update
    @PostMapping("/manual/update")
    public ResponseEntity<String> updateManually(@RequestBody ManualUpdateRequest req) {
        // Postman Body 예시: { "date": "...", "mealType": "...", "menus": ["밥", "국"...], "reason": "..." }
        mealPlanService.updateMenuManually(req.date, req.mealType, req.menus, req.reason);
        return ResponseEntity.ok("Manually updated successfully");
    }

    // --- DTO 변환 메서드 ---
    private MealMenuResponse toMealMenuResponse(MealPlanMenu menu) {
        return new MealMenuResponse(
                menu.getId(),
                menu.getMenuDate(),
                menu.getMealType().name(),
                menu.getRice(), menu.getSoup(), menu.getMain1(), menu.getMain2(),
                menu.getSide(), menu.getKimchi(), menu.getDessert(),
                parseRawMenus(menu.getRawMenusJson()),
                (int) Math.round(menu.getKcal() != null ? menu.getKcal() : 0),
                (int) Math.round(menu.getCarb() != null ? menu.getCarb() : 0),
                (int) Math.round(menu.getProt() != null ? menu.getProt() : 0),
                (int) Math.round(menu.getFat() != null ? menu.getFat() : 0), menu.getCost(),
                menu.getRawMenusJson()
        );
    }

    private List<String> parseRawMenus(String rawMenusJson) {
        try {
            if (rawMenusJson == null || rawMenusJson.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(rawMenusJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<MenuHistory>> getAllHistory() {
        List<MenuHistory> histories = menuHistoryRepository.findAllByOrderByIdDesc();
        return ResponseEntity.ok(histories);
    }

    // --- 수동 수정용 DTO ---
    public record ManualUpdateRequest(String date, String mealType, List<String> menus, String reason) {}

}