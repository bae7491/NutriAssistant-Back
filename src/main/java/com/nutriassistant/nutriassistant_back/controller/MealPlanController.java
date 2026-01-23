package com.nutriassistant.nutriassistant_back.controller;

import com.nutriassistant.nutriassistant_back.DTO.MealPlanCreateRequest;
import com.nutriassistant.nutriassistant_back.entity.MealPlan;
import com.nutriassistant.nutriassistant_back.entity.MealPlanMenu;
import java.util.List;
import com.nutriassistant.nutriassistant_back.service.MealPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/mealplans", "/mealplan"})
public class MealPlanController {

    private final MealPlanService mealPlanService;

    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MealPlanCreateRequest request) {
        MealPlan saved = mealPlanService.createOrReplace(request);
        return ResponseEntity.ok(new MealPlanIdResponse(saved.getId()));
    }

    // POST /mealplan/generate?year=2026&month=3
    // (also available as /api/mealplans/generate?year=...&month=...)
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestParam int year, @RequestParam int month) {
        // This service method should call FastAPI to generate the monthly plan and persist it,
        // then return the saved MealPlan (or its id).
        MealPlan saved = mealPlanService.generateAndSave(year, month);
        return ResponseEntity.ok(new MealPlanIdResponse(saved.getId()));
    }

    @GetMapping("/{id}")
    public MealPlanResponse getMealPlan(@PathVariable Long id) {
        MealPlan plan = mealPlanService.getById(id);

        List<MealPlanMenuResponse> menus = plan.getMenus().stream()
                .map(this::toMenuResponse)
                .toList();

        return new MealPlanResponse(
                plan.getId(),
                plan.getYear(),
                plan.getMonth(),
                plan.getGeneratedAt(),
                menus
        );
    }

    private MealPlanMenuResponse toMenuResponse(MealPlanMenu m) {
        return new MealPlanMenuResponse(
                m.getId(),
                m.getMenuDate(),
                m.getMealType(),
                m.getRice(),
                m.getSoup(),
                m.getMain1(),
                m.getMain2(),
                m.getSide(),
                m.getKimchi(),
                m.getKcal(),
                m.getProt()
        );
    }

    public record MealPlanIdResponse(Long mealPlanId) {}

    public record MealPlanMenuResponse(
            Long menuId,
            java.time.LocalDate menuDate,
            com.nutriassistant.nutriassistant_back.entity.MealType mealType,
            String rice,
            String soup,
            String main1,
            String main2,
            String side,
            String kimchi,
            Integer kcal,
            Integer prot
    ) {}

    public record MealPlanResponse(
            Long mealPlanId,
            int year,
            int month,
            java.time.LocalDateTime generatedAt,
            List<MealPlanMenuResponse> menus
    ) {}
}