package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 식단 메뉴 조회용 DTO
 */
public record MealDto(
        Long id,
        LocalDate menuDate,
        MealType mealType,
        String riceDisplay,
        String soupDisplay,
        String main1Display,
        String main2Display,
        String sideDisplay,
        String kimchiDisplay,
        String dessertDisplay,
        BigDecimal kcal,
        BigDecimal carb,
        BigDecimal prot,
        BigDecimal fat,
        Integer cost,
        String aiComment
) {
    /**
     * Entity → DTO 변환
     */
    public static MealDto from(MealPlanMenu menu) {
        if (menu == null) {
            return null;
        }

        return new MealDto(
                menu.getId(),
                menu.getMenuDate(),
                menu.getMealType(),
                menu.getRiceDisplay(),
                menu.getSoupDisplay(),
                menu.getMain1Display(),
                menu.getMain2Display(),
                menu.getSideDisplay(),
                menu.getKimchiDisplay(),
                menu.getDessertDisplay(),
                menu.getKcal(),
                menu.getCarb(),
                menu.getProt(),
                menu.getFat(),
                menu.getCost(),
                menu.getAiComment()
        );
    }
}