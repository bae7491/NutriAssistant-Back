package com.nutriassistant.nutriassistant_back.DTO;

import com.nutriassistant.nutriassistant_back.entity.MealPlanMenu;

public record MealDto(
        String rice,
        String soup,
        String main1,
        String main2,
        String side,
        String kimchi,
        Double kcal,
        Double prot
) {
    public static MealDto from(MealPlanMenu m) {
        if (m == null) return null;
        return new MealDto(
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
}
