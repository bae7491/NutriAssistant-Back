package com.nutriassistant.nutriassistant_back.controller;

import com.nutriassistant.nutriassistant_back.DTO.MealMenuResponse;
import com.nutriassistant.nutriassistant_back.DTO.MealPlanResponse;
import com.nutriassistant.nutriassistant_back.entity.MealPlan;
import com.nutriassistant.nutriassistant_back.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.service.MealPlanService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/mealplan")
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final ObjectMapper objectMapper;

    public MealPlanController(MealPlanService mealPlanService, ObjectMapper objectMapper) {
        this.mealPlanService = mealPlanService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{id}")
    public MealPlanResponse getOne(@PathVariable Long id) {
        MealPlan plan = mealPlanService.getById(id); // ✅ findByIdWithMenus 사용 중

        List<MealMenuResponse> menus = plan.getMenus().stream()
                .map(this::toMealMenuResponse)
                .toList();

        return new MealPlanResponse(
                plan.getId(),
                plan.getYear(),
                plan.getMonth(),
                plan.getGeneratedAt(),
                menus
        );
    }

    private MealMenuResponse toMealMenuResponse(MealPlanMenu menu) {
        return new MealMenuResponse(
                menu.getId(),
                menu.getMenuDate(),
                menu.getMealType().name(),
                menu.getRice(),
                menu.getSoup(),
                menu.getMain1(),
                menu.getMain2(),
                menu.getSide(),
                menu.getKimchi(),
                menu.getDessert(),
                parseRawMenus(menu.getRawMenusJson()),
                menu.getKcal(),
                menu.getCarb(),
                menu.getProt(),
                menu.getFat(),
                menu.getCost(),
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
}