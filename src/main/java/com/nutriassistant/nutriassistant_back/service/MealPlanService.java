package com.nutriassistant.nutriassistant_back.service;

import com.nutriassistant.nutriassistant_back.DTO.MealPlanCreateRequest;
import com.nutriassistant.nutriassistant_back.entity.MealPlan;
import com.nutriassistant.nutriassistant_back.repository.MealPlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanMenuService mealPlanMenuService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * FastAPI 서버 베이스 URL
     * - 기본값: http://localhost:8001
     * - application.yml/properties에서 fastapi.base-url 로 덮어쓸 수 있음
     */
    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    public MealPlanService(MealPlanRepository mealPlanRepository,
                           MealPlanMenuService mealPlanMenuService,
                           RestTemplate restTemplate,
                           ObjectMapper objectMapper) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanMenuService = mealPlanMenuService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * FastAPI에 월별 식단 생성 요청을 보내고, 응답(JSON)을 받아 DB에 저장한 뒤 저장된 MealPlan을 반환
     */
    @Transactional
    public MealPlan generateAndSave(int year, int month) {
        // FastAPI 엔드포인트: POST /v1/menus/month:generate
        // FastAPI는 query param이 아니라 JSON body로 year/month를 받음
        String url = String.format("%s/month/generate", fastApiBaseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> body = Map.of(
                "year", year,
                "month", month,
                // options는 FastAPI 스키마상 존재하므로 빈 객체라도 보내는 게 안전
                "options", Map.of()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<MealPlanCreateRequest> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                MealPlanCreateRequest.class
        );

        MealPlanCreateRequest req = response.getBody();
        if (req == null) {
            throw new IllegalStateException("FastAPI 응답 바디가 비어있습니다. url=" + url);
        }

        return createOrReplace(req);
    }

    @Transactional
    public MealPlan createOrReplace(MealPlanCreateRequest req) {
        // 1) (year, month) 단위로 meal_plan은 1개만 유지(Unique 제약 대응)
        MealPlan mealPlan = mealPlanRepository.findByYearAndMonth(req.year(), req.month())
                .orElseGet(() -> new MealPlan(req.year(), req.month(), req.generatedAt()));

        // 기존 월 플랜이 있으면 generatedAt 갱신
        mealPlan.setGeneratedAt(req.generatedAt());

        // 먼저 meal_plan 저장(신규면 id 생성)
        MealPlan savedPlan = mealPlanRepository.save(mealPlan);

        // FastAPI 응답에서 내려온 meals[]를 meal_plan_menu에 저장/교체
        JsonNode payload = objectMapper.valueToTree(req);
        mealPlanMenuService.importFromFastApi(savedPlan.getId(), payload);

        return savedPlan;
    }

    @Transactional(readOnly = true)
    public MealPlan getById(Long id) {
        return mealPlanRepository.findByIdWithMenus(id)
                .orElseThrow(() -> new IllegalArgumentException("MealPlan not found: " + id));
    }

}