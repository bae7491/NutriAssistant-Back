package com.nutriassistant.nutriassistant_back.DTO;

import java.util.Map;

// 단가 DB 전체 응답
public record MenuCostDatabaseResponse(
        Integer year,
        Integer totalMenus,
        Map<String, Integer> prices
) {}
