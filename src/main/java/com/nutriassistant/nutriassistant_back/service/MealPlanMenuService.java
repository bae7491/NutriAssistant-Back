package com.nutriassistant.nutriassistant_back.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nutriassistant.nutriassistant_back.entity.MealPlan;
import com.nutriassistant.nutriassistant_back.repository.MealPlanRepository;
import org.springframework.transaction.annotation.Transactional;

import com.nutriassistant.nutriassistant_back.DTO.MealMenuResponse;
import com.nutriassistant.nutriassistant_back.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.entity.MealType;
import com.nutriassistant.nutriassistant_back.repository.MealPlanMenuRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private List<String> parseRawMenus(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
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

    /**
     * date + type 로 해당 날짜/타입의 최신(가장 최근에 생성된) 메뉴 1건을 조회
     */
    public MealMenuResponse getOne(LocalDate date, MealType type) {
        MealPlanMenu menu = mealPlanMenuRepository
                .findFirstByMenuDateAndMealTypeOrderByMenuDateDescIdDesc(date, type)
                .orElseThrow(() -> new IllegalArgumentException(
                        "menu not found: date=" + date + ", type=" + type
                ));

        // 응답에는 mealPlanId가 아니라 menuId를 내려주도록 구성
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

    @Transactional
    public void importFromFastApi(Long mealPlanId, JsonNode payload) {
        if (payload == null) return;

        JsonNode list = payload.path("meals");
        if (!list.isArray()) list = payload.path("menus");
        if (!list.isArray()) return;

        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new IllegalArgumentException("MealPlan not found: " + mealPlanId));

        mealPlanMenuRepository.deleteByMealPlan_Id(mealPlanId);

        for (JsonNode m : list) {
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

            menu.setRice(m.path("Rice").isNull() ? null : m.path("Rice").asText(null));
            menu.setSoup(m.path("Soup").isNull() ? null : m.path("Soup").asText(null));
            menu.setMain1(m.path("Main1").isNull() ? null : m.path("Main1").asText(null));
            menu.setMain2(m.path("Main2").isNull() ? null : m.path("Main2").asText(null));
            menu.setSide(m.path("Side").isNull() ? null : m.path("Side").asText(null));
            menu.setKimchi(m.path("Kimchi").isNull() ? null : m.path("Kimchi").asText(null));
            menu.setDessert(m.path("Dessert").isNull() ? null : m.path("Dessert").asText(null));

            if (m.hasNonNull("Kcal")) menu.setKcal((int) Math.round(m.get("Kcal").asDouble()));
            if (m.hasNonNull("Carb")) menu.setCarb((int) Math.round(m.get("Carb").asDouble()));
            if (m.hasNonNull("Prot")) menu.setProt((int) Math.round(m.get("Prot").asDouble()));
            if (m.hasNonNull("Fat")) menu.setFat((int) Math.round(m.get("Fat").asDouble()));
            if (m.hasNonNull("Cost")) menu.setCost((int) Math.round(m.get("Cost").asDouble()));

            // RawMenus -> JSON 저장 (있으면)
            if (m.has("RawMenus") && m.get("RawMenus").isArray()) {
                menu.setRawMenusJson(m.get("RawMenus").toString());
            }

            mealPlanMenuRepository.save(menu);
        }
    }
}