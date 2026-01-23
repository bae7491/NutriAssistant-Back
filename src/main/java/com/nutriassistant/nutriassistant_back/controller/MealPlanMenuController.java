package com.nutriassistant.nutriassistant_back.controller;

import com.nutriassistant.nutriassistant_back.DTO.MealMenuResponse;
import com.nutriassistant.nutriassistant_back.entity.MealType;
import com.nutriassistant.nutriassistant_back.service.MealPlanMenuService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/mealplan")
public class MealPlanMenuController {

    private final MealPlanMenuService mealPlanMenuService;

    public MealPlanMenuController(MealPlanMenuService mealPlanMenuService) {
        this.mealPlanMenuService = mealPlanMenuService;
    }

    // GET /mealplan/menus/one?date=2026-04-07&type=LUNCH
    @GetMapping("/menus/one")
    public MealMenuResponse getOne(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("type") MealType type
    ) {
        return mealPlanMenuService.getOne(date, type);
    }

    // 매핑 확인용 (여기가 404면 컨트롤러가 스캔/등록 자체가 안 된 것)
    @GetMapping("/menus/ping")
    public String ping() {
        return "OK";
    }
}