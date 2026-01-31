package com.nutriassistant.nutriassistant_back.domain.MealPlan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealPlan;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealType;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.MealPlanMenuRepository;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.MealPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MealPlanMenuService {

    private final MealPlanMenuRepository mealPlanMenuRepository;
    private final MealPlanRepository mealPlanRepository;
    private final ObjectMapper objectMapper;

    public MealPlanMenuService(MealPlanMenuRepository mealPlanMenuRepository,
                               MealPlanRepository mealPlanRepository,
                               ObjectMapper objectMapper) {
        this.mealPlanMenuRepository = mealPlanMenuRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * FastAPI(한글) 타입 문자열을 MealType으로 변환
     */
    private MealType toMealType(String type) {
        if (type == null) return null;
        String t = type.trim();
        // 한글 매핑
        if (t.equals("중식") || t.equalsIgnoreCase("lunch")) return MealType.LUNCH;
        if (t.equals("석식") || t.equalsIgnoreCase("dinner")) return MealType.DINNER;
        // 혹시 Enum name 그대로 오는 경우
        try {
            return MealType.valueOf(t.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid meal type: " + type);
        }
    }

    @Transactional
    public void importFromFastApi(Long mealPlanId, JsonNode payload) {
        if (payload == null) return;

        JsonNode list = payload.path("meals");
        if (!list.isArray()) list = payload.path("menus");
        if (!list.isArray()) return;

        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new IllegalArgumentException("MealPlan not found: " + mealPlanId));

        mealPlanMenuRepository.deleteByMealPlan_Id(mealPlanId);
        mealPlanMenuRepository.flush();  // 삭제 쿼리를 즉시 실행하여 중복 키 에러 방지

        // 정렬: 날짜 오름차순 → 타입 오름차순
        List<JsonNode> sortedList = new ArrayList<>();
        list.forEach(sortedList::add);

        sortedList.sort((a, b) -> {
            String dateA = a.path("Date").asText("");
            String dateB = b.path("Date").asText("");
            int dateCompare = dateA.compareTo(dateB);

            if (dateCompare != 0) {
                return dateCompare;
            }

            String typeA = a.path("Type").asText("");
            String typeB = b.path("Type").asText("");

            if (typeA.equals("중식") && typeB.equals("석식")) return -1;
            if (typeA.equals("석식") && typeB.equals("중식")) return 1;

            return 0;
        });

        for (JsonNode m : sortedList) {
            String dateStr = m.hasNonNull("Date") ? m.get("Date").asText() : null;
            String typeStr = m.hasNonNull("Type") ? m.get("Type").asText() : null;
            if (dateStr == null || typeStr == null) continue;

            LocalDate date = LocalDate.parse(dateStr);
            MealType mealType = toMealType(typeStr);
            if (mealType == null) continue;

            MealPlanMenu menu = new MealPlanMenu();
            menu.setMealPlan(mealPlan);
            menu.setMenuDate(date);
            menu.setMealType(mealType);

            menu.setRiceDisplay(m.path("Rice").isNull() ? null : m.path("Rice").asText(null));
            menu.setSoupDisplay(m.path("Soup").isNull() ? null : m.path("Soup").asText(null));
            menu.setMain1Display(m.path("Main1").isNull() ? null : m.path("Main1").asText(null));
            menu.setMain2Display(m.path("Main2").isNull() ? null : m.path("Main2").asText(null));
            menu.setSideDisplay(m.path("Side").isNull() ? null : m.path("Side").asText(null));
            menu.setKimchiDisplay(m.path("Kimchi").isNull() ? null : m.path("Kimchi").asText(null));
            menu.setDessertDisplay(m.path("Dessert").isNull() ? null : m.path("Dessert").asText(null));

            if (m.hasNonNull("Kcal")) menu.setKcal(BigDecimal.valueOf(Math.round(m.get("Kcal").asDouble())));
            if (m.hasNonNull("Carb")) menu.setCarb(BigDecimal.valueOf(Math.round(m.get("Carb").asDouble())));
            if (m.hasNonNull("Prot")) menu.setProt(BigDecimal.valueOf(Math.round(m.get("Prot").asDouble())));
            if (m.hasNonNull("Fat")) menu.setFat(BigDecimal.valueOf(Math.round(m.get("Fat").asDouble())));
            if (m.hasNonNull("Cost")) menu.setCost((int) Math.round(m.get("Cost").asDouble()));

            if (m.has("RawMenus") && m.get("RawMenus").isArray()) {
                menu.setRawMenusJson(m.get("RawMenus").toString());
            }

            mealPlanMenuRepository.save(menu);
        }
    }
}