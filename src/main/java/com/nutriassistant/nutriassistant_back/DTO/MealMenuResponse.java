package com.nutriassistant.nutriassistant_back.DTO;

import java.time.LocalDate;

public record MealMenuResponse(
        Long menuId,
        LocalDate date,
        String type,
        String rice,
        String soup,
        String main1,
        String main2,
        String side,
        String kimchi,
        Integer kcal,
        Integer prot
) {

    public MealMenuResponse(
            Long id,
            LocalDate menuDate,
            String name,
            String rice,
            String soup,
            String main1,
            String main2,
            String side,
            String kimchi,
            Double kcal,
            Double prot
    ) {
        this(
                id,
                menuDate,
                name,
                rice,
                soup,
                main1,
                main2,
                side,
                kimchi,
                kcal != null ? kcal.intValue() : null,
                prot != null ? prot.intValue() : null
        );
    }
}