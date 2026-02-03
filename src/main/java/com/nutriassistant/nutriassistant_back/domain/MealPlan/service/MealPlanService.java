package com.nutriassistant.nutriassistant_back.domain.MealPlan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.*;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.*;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.FoodInfoRepository;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.MealPlanMenuRepository;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.MealPlanRepository;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.MenuHistoryRepository;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.entity.NewFoodInfo;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.repository.NewFoodInfoRepository;

// [수정 1] Report 관련 import 제거 -> MonthlyOpsDoc(운영일지) 관련 import 추가
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity.MonthlyOpsDoc;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.service.MonthlyOpsDocService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MealPlanService {

    // --- Repository & Service 의존성 주입 ---
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanMenuRepository mealPlanMenuRepository;
    private final MenuHistoryRepository menuHistoryRepository;
    private final MealPlanMenuService mealPlanMenuService;

    // [변경] RestTemplate 대신 최신 RestClient 사용
    private final RestClient restClient;

    private final ObjectMapper objectMapper;
    private final FoodInfoRepository foodInfoRepository;
    private final NewFoodInfoRepository newFoodInfoRepository;

    // [수정 2] ReportService 제거하고 MonthlyOpsDocService 주입
    // private final ReportService reportService; (삭제됨)
    private final MonthlyOpsDocService monthlyOpsDocService;

    // --- 환경 변수 ---
    @Value("${fastapi.base-url:http://localhost:8001}")
    private String fastApiBaseUrl;

    @Value("${fastapi.internal-token:}")
    private String internalToken;

    // [수정 3] 생성자 주입 변경 (ReportService -> MonthlyOpsDocService)
    public MealPlanService(MealPlanRepository mealPlanRepository,
                           MealPlanMenuRepository mealPlanMenuRepository,
                           MenuHistoryRepository menuHistoryRepository,
                           MealPlanMenuService mealPlanMenuService,
                           MonthlyOpsDocService monthlyOpsDocService, // <-- 변경됨
                           RestClient restClient,
                           ObjectMapper objectMapper,
                           FoodInfoRepository foodInfoRepository,
                           NewFoodInfoRepository newFoodInfoRepository
    ) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanMenuRepository = mealPlanMenuRepository;
        this.menuHistoryRepository = menuHistoryRepository;
        this.mealPlanMenuService = mealPlanMenuService;
        this.monthlyOpsDocService = monthlyOpsDocService; // <-- 할당
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.foodInfoRepository = foodInfoRepository;
        this.newFoodInfoRepository = newFoodInfoRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MealPlan> findById(Long mealPlanId) {
        return mealPlanRepository.findById(mealPlanId);
    }

    @Transactional(readOnly = true)
    public Optional<MealPlan> findBySchoolIdAndYearAndMonth(Long schoolId, Integer year, Integer month) {
        return mealPlanRepository.findBySchoolIdAndYearAndMonth(schoolId, year, month);
    }

    // =========================================================================
    // 1. [생성] 월간 식단 생성 (FastAPI 호출 -> DB 저장)
    // =========================================================================
    @Transactional
    public MealPlan generateAndSave(Long schoolId, MealPlanGenerateRequest req) {
        log.info("============================================================");
        log.info("📋 식단 생성 요청 시작");
        log.info("============================================================");
        log.info("   학교 ID: {}", schoolId);
        log.info("   연도/월: {}/{}", req.getYear(), req.getMonth());

        Integer year = Integer.parseInt(req.getYear());
        Integer month = Integer.parseInt(req.getMonth());

        // ========================================
        // [수정 4] DB에서 이전 달 운영 일지(MonthlyOpsDoc) 조회
        // ========================================
        JsonNode reportData = null;

        // 이전 달 계산
        int reportYear = year;
        int reportMonth = month - 1;
        if (reportMonth == 0) {
            reportMonth = 12;
            reportYear -= 1;
        }

        log.info("📊 운영 일지(MonthlyOpsDoc) 조회 시도: {}년 {}월", reportYear, reportMonth);

        // [핵심 변경] reportService -> monthlyOpsDocService 호출
        Optional<MonthlyOpsDoc> docOpt = monthlyOpsDocService.findByYearAndMonth(
                reportYear, reportMonth
        );

        if (docOpt.isPresent()) {
            // 운영 일지 JSON 데이터 추출
            reportData = monthlyOpsDocService.getReportDataAsJson(docOpt.get());
            log.info("✅ 운영 일지 발견 → FastAPI로 전달 (가중치 분석 예정)");
        } else {
            log.info("ℹ️ 운영 일지 없음 → 기본 가중치로 식단 생성");
        }

        // ========================================
        // 2. FastAPI 요청 Body 구성
        // ========================================
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("year", year);
        requestBody.put("month", month);
        requestBody.put("school_id", schoolId);

        // Options 추가
        if (req.getOptions() != null) {
            Map<String, Object> options = new HashMap<>();
            options.put("numGenerations", req.getOptions().getNumGenerations());

            if (req.getOptions().getConstraints() != null) {
                Map<String, Object> constraints = new HashMap<>();
                MealPlanGenerateRequest.Constraints c = req.getOptions().getConstraints();

                if (c.getNutritionKey() != null) constraints.put("nutrition_key", c.getNutritionKey().name());
                if (c.getTargetPrice() != null) constraints.put("target_price", c.getTargetPrice());
                if (c.getMaxPriceLimit() != null) constraints.put("max_price_limit", c.getMaxPriceLimit());
                if (c.getCookStaff() != null) constraints.put("cook_staff", c.getCookStaff());
                if (c.getFacilityText() != null) constraints.put("facility_text", c.getFacilityText());

                options.put("constraints", constraints);
            }
            requestBody.put("options", options);
        }

        // [중요] FastAPI는 여전히 "report"라는 키로 데이터를 받기를 원하므로 키 이름은 유지
        if (reportData != null) {
            requestBody.put("report", objectMapper.convertValue(reportData, Map.class));
        }

        // ========================================
        // 2-1. 신메뉴 DB 조회 및 추가
        // ========================================
        List<NewFoodInfo> newFoodInfoList = newFoodInfoRepository.findByDeletedFalse();
        if (!newFoodInfoList.isEmpty()) {
            List<Map<String, Object>> newMenus = newFoodInfoList.stream()
                    .map(this::convertNewFoodInfoToMap)
                    .collect(Collectors.toList());
            requestBody.put("new_menus", newMenus);
            log.info("📋 신메뉴 {}개 추가", newMenus.size());
        }

        // ========================================
        // 3. FastAPI 호출 (RestClient 사용)
        // ========================================
        log.info("🚀 FastAPI 호출: /month/generate");

        JsonNode fastPayload;
        try {
            fastPayload = restClient.post()
                    .uri("/month/generate")
                    .headers(httpHeaders -> httpHeaders.addAll(createHeaders()))
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.error("FastAPI 호출 실패", e);
            throw new RuntimeException("식단 생성 중 AI 서버 오류 발생: " + e.getMessage());
        }

        log.info("✅ FastAPI 응답 수신");

        // ========================================
        // 4. DB 저장
        // ========================================
        MealPlanCreateRequest saveReq = new MealPlanCreateRequest(
                schoolId,
                year,
                month,
                LocalDateTime.now(),
                fastPayload.get("meals")
        );

        MealPlan savedPlan = createOrReplace(saveReq);

        log.info("============================================================");
        log.info("✅ 식단 생성 완료: MealPlan ID={}", savedPlan.getId());
        log.info("============================================================");

        return savedPlan;
    }

    // =========================================================================
    // 2. [저장 로직] 식단 데이터 저장 및 갱신
    // =========================================================================
    @Transactional
    public MealPlan createOrReplace(MealPlanCreateRequest req) {
        log.info("💾 DB 저장: {}년 {}월 (학교 ID: {})", req.year(), req.month(), req.schoolId());

        MealPlan mealPlan = mealPlanRepository.findBySchoolIdAndYearAndMonth(
                req.schoolId(), req.year(), req.month()
        ).orElseGet(() -> {
            log.info("   신규 MealPlan 생성");
            return new MealPlan(req.schoolId(), req.year(), req.month());
        });

        if (mealPlan.getId() != null) {
            log.info("   기존 MealPlan 갱신: ID={}", mealPlan.getId());
        }

        MealPlan savedPlan = mealPlanRepository.save(mealPlan);

        log.info("✅ MealPlan 저장: ID={}", savedPlan.getId());

        JsonNode payload = objectMapper.createObjectNode()
                .set("meals", req.menus());

        log.info("💾 메뉴 저장 시작...");
        mealPlanMenuService.importFromFastApi(savedPlan.getId(), payload);
        log.info("✅ 메뉴 저장 완료");

        return savedPlan;
    }

    // =========================================================================
    // 3. [헬퍼] 공통 내부 메서드
    // =========================================================================
    private void saveHistory(String date, String type, String oldM, String newM, String reason,
                             MenuHistory.ActionType action, LocalDateTime menuCreatedAt) {
        MenuHistory history = MenuHistory.builder()
                .mealDate(date)
                .mealType(type)
                .oldMenus(oldM)
                .newMenus(newM)
                .reason(reason)
                .actionType(action)
                .menuCreatedAt(menuCreatedAt)
                .build();
        menuHistoryRepository.save(history);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalToken != null && !internalToken.isEmpty()) {
            headers.set("X-Internal-Token", internalToken);
        }
        return headers;
    }

    private Map<String, Object> convertNewFoodInfoToMap(NewFoodInfo info) {
        Map<String, Object> map = new HashMap<>();
        map.put("food_code", info.getFoodCode());
        map.put("food_name", info.getFoodName());
        map.put("category", info.getCategory());
        map.put("serving_basis", info.getServingBasis());
        map.put("food_weight", info.getFoodWeight());
        map.put("kcal", info.getKcal());
        map.put("protein", info.getProtein());
        map.put("fat", info.getFat());
        map.put("carbs", info.getCarbs());
        map.put("calcium", info.getCalcium());
        map.put("iron", info.getIron());
        map.put("vitamin_a", info.getVitaminA());
        map.put("thiamin", info.getThiamin());
        map.put("riboflavin", info.getRiboflavin());
        map.put("vitamin_c", info.getVitaminC());
        map.put("ingredients", info.getIngredients());
        map.put("allergy_info", info.getAllergyInfo());
        map.put("recipe", info.getRecipe());
        return map;
    }

    // =========================================================================
    // 4. [응답 변환] MealPlan -> MealPlanGenerateResponse 리스트 변환
    // =========================================================================
    public List<MealPlanGenerateResponse> toResponseList(MealPlan mealPlan) {
        List<MealPlanMenu> menus = mealPlanMenuRepository.findAllByMealPlanIdOrderByMenuDateAscMealTypeAsc(mealPlan.getId());

        menus.sort(Comparator
                .comparing(MealPlanMenu::getMenuDate)
                .thenComparing(m -> m.getMealType().ordinal()));

        return menus.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MealPlanGenerateResponse toResponse(MealPlanMenu menu) {
        MealPlanGenerateResponse.MenuItem riceItem = parseMenuItem(menu.getRiceDisplay());
        MealPlanGenerateResponse.MenuItem soupItem = parseMenuItem(menu.getSoupDisplay());
        MealPlanGenerateResponse.MenuItem main1Item = parseMenuItem(menu.getMain1Display());
        MealPlanGenerateResponse.MenuItem main2Item = parseMenuItem(menu.getMain2Display());
        MealPlanGenerateResponse.MenuItem sideItem = parseMenuItem(menu.getSideDisplay());
        MealPlanGenerateResponse.MenuItem kimchiItem = parseMenuItem(menu.getKimchiDisplay());
        MealPlanGenerateResponse.MenuItem dessertItem = parseMenuItem(menu.getDessertDisplay());

        MealPlanGenerateResponse.MenuItems menuItems = MealPlanGenerateResponse.MenuItems.builder()
                .rice(riceItem)
                .soup(soupItem)
                .main1(main1Item)
                .main2(main2Item)
                .side(sideItem)
                .kimchi(kimchiItem)
                .dessert(dessertItem)
                .build();

        MealPlanGenerateResponse.AllergenSummary allergenSummary = buildAllergenSummary(
                riceItem, soupItem, main1Item, main2Item, sideItem, kimchiItem, dessertItem
        );

        return MealPlanGenerateResponse.builder()
                .id(menu.getId())
                .date(menu.getMenuDate())
                .mealType(menu.getMealType().name())
                .kcal(menu.getKcal() != null ? menu.getKcal() : BigDecimal.ZERO)
                .carb(menu.getCarb() != null ? menu.getCarb() : BigDecimal.ZERO)
                .prot(menu.getProt() != null ? menu.getProt() : BigDecimal.ZERO)
                .fat(menu.getFat() != null ? menu.getFat() : BigDecimal.ZERO)
                .cost(menu.getCost())
                .aiComment(menu.getAiComment())
                .menuItems(menuItems)
                .allergenSummary(allergenSummary)
                .build();
    }

    private static final Pattern ALLERGEN_PATTERN = Pattern.compile("(.+?)\\(([\\d,\\s]+)\\)$");

    private MealPlanGenerateResponse.MenuItem parseMenuItem(String display) {
        if (display == null || display.isBlank()) {
            return null;
        }

        String name = display;
        List<Integer> allergens = new ArrayList<>();

        Matcher matcher = ALLERGEN_PATTERN.matcher(display.trim());
        if (matcher.matches()) {
            name = matcher.group(1).trim();
            String allergenStr = matcher.group(2);
            for (String s : allergenStr.split(",")) {
                try {
                    allergens.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        Long menuId = null;
        Optional<FoodInfo> foodOpt = foodInfoRepository.findByFoodNameIgnoreSpace(name);
        if (foodOpt.isPresent()) {
            menuId = foodOpt.get().getId();
        }

        return MealPlanGenerateResponse.MenuItem.builder()
                .menuId(menuId)
                .name(name)
                .display(display)
                .allergens(allergens)
                .build();
    }

    private MealPlanGenerateResponse.AllergenSummary buildAllergenSummary(
            MealPlanGenerateResponse.MenuItem... items
    ) {
        Set<Integer> uniqueAllergens = new TreeSet<>();
        Map<String, List<Integer>> byMenu = new LinkedHashMap<>();

        for (MealPlanGenerateResponse.MenuItem item : items) {
            if (item != null && item.getAllergens() != null && !item.getAllergens().isEmpty()) {
                uniqueAllergens.addAll(item.getAllergens());
                byMenu.put(item.getName(), item.getAllergens());
            }
        }

        return MealPlanGenerateResponse.AllergenSummary.builder()
                .uniqueAllergens(new ArrayList<>(uniqueAllergens))
                .byMenu(byMenu)
                .build();
    }

    // =========================================================================
    // 5. [응답 변환] MealPlan -> MealPlanMonthlyResponse 변환
    // =========================================================================
    public MealPlanMonthlyResponse toMonthlyResponse(MealPlan mealPlan) {
        List<MealPlanMenu> menus = mealPlanMenuRepository.findByMealPlanIdOrderByMenuDateAscMealTypeAsc(mealPlan.getId());

        menus.sort(Comparator
                .comparing(MealPlanMenu::getMenuDate)
                .thenComparing(m -> m.getMealType().ordinal()));

        List<MealPlanMonthlyResponse.MenuDetail> menuDetails = menus.stream()
                .map(this::toMenuDetail)
                .collect(Collectors.toList());

        return MealPlanMonthlyResponse.builder()
                .mealPlanId(mealPlan.getId())
                .year(mealPlan.getYear())
                .month(mealPlan.getMonth())
                .schoolId(mealPlan.getSchoolId())
                .createdAt(mealPlan.getCreatedAt())
                .updatedAt(mealPlan.getUpdatedAt())
                .menus(menuDetails)
                .build();
    }

    private MealPlanMonthlyResponse.MenuDetail toMenuDetail(MealPlanMenu menu) {
        MealPlanMonthlyResponse.MenuItem riceItem = parseMonthlyMenuItem(menu.getRiceDisplay());
        MealPlanMonthlyResponse.MenuItem soupItem = parseMonthlyMenuItem(menu.getSoupDisplay());
        MealPlanMonthlyResponse.MenuItem main1Item = parseMonthlyMenuItem(menu.getMain1Display());
        MealPlanMonthlyResponse.MenuItem main2Item = parseMonthlyMenuItem(menu.getMain2Display());
        MealPlanMonthlyResponse.MenuItem sideItem = parseMonthlyMenuItem(menu.getSideDisplay());
        MealPlanMonthlyResponse.MenuItem kimchiItem = parseMonthlyMenuItem(menu.getKimchiDisplay());
        MealPlanMonthlyResponse.MenuItem dessertItem = parseMonthlyMenuItem(menu.getDessertDisplay());

        MealPlanMonthlyResponse.MenuItems menuItems = MealPlanMonthlyResponse.MenuItems.builder()
                .rice(riceItem)
                .soup(soupItem)
                .main1(main1Item)
                .main2(main2Item)
                .side(sideItem)
                .kimchi(kimchiItem)
                .dessert(dessertItem)
                .build();

        MealPlanMonthlyResponse.Nutrition nutrition = MealPlanMonthlyResponse.Nutrition.builder()
                .kcal(menu.getKcal() != null ? menu.getKcal().intValue() : 0)
                .carb(menu.getCarb() != null ? menu.getCarb().intValue() : 0)
                .prot(menu.getProt() != null ? menu.getProt().intValue() : 0)
                .fat(menu.getFat() != null ? menu.getFat().intValue() : 0)
                .build();

        MealPlanMonthlyResponse.AllergenSummary allergenSummary = buildMonthlyAllergenSummary(
                riceItem, soupItem, main1Item, main2Item, sideItem, kimchiItem, dessertItem
        );

        return MealPlanMonthlyResponse.MenuDetail.builder()
                .menuId(menu.getId())
                .date(menu.getMenuDate())
                .mealType(menu.getMealType().name())
                .nutrition(nutrition)
                .cost(menu.getCost())
                .aiComment(menu.getAiComment())
                .menuItems(menuItems)
                .allergenSummary(allergenSummary)
                .build();
    }

    private MealPlanMonthlyResponse.MenuItem parseMonthlyMenuItem(String display) {
        if (display == null || display.isBlank()) {
            return null;
        }

        String name = display;
        List<Integer> allergens = new ArrayList<>();

        Matcher matcher = ALLERGEN_PATTERN.matcher(display.trim());
        if (matcher.matches()) {
            name = matcher.group(1).trim();
            String allergenStr = matcher.group(2);
            for (String s : allergenStr.split(",")) {
                try {
                    allergens.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        String foodCode = null;
        Optional<FoodInfo> foodOpt = foodInfoRepository.findByFoodNameIgnoreSpace(name);
        if (foodOpt.isPresent()) {
            foodCode = foodOpt.get().getFoodCode();
        }

        return MealPlanMonthlyResponse.MenuItem.builder()
                .id(foodCode)
                .name(name)
                .display(display)
                .allergens(allergens)
                .build();
    }

    private MealPlanMonthlyResponse.AllergenSummary buildMonthlyAllergenSummary(
            MealPlanMonthlyResponse.MenuItem... items
    ) {
        Set<Integer> uniqueAllergens = new TreeSet<>();

        for (MealPlanMonthlyResponse.MenuItem item : items) {
            if (item != null && item.getAllergens() != null && !item.getAllergens().isEmpty()) {
                uniqueAllergens.addAll(item.getAllergens());
            }
        }

        return MealPlanMonthlyResponse.AllergenSummary.builder()
                .uniqueAllergens(new ArrayList<>(uniqueAllergens))
                .hasAllergen5(uniqueAllergens.contains(5))
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<MealPlanMenu> findByDateAndMealType(Long schoolId, LocalDate menuDate, MealType mealType) {
        return mealPlanMenuRepository.findByMealPlan_SchoolIdAndMenuDateAndMealType(schoolId, menuDate, mealType);
    }

    public MealPlanDetailResponse toDetailResponse(MealPlanMenu menu) {
        MealPlanDetailResponse.MenuItem riceItem = parseDetailMenuItem(menu.getRiceDisplay());
        MealPlanDetailResponse.MenuItem soupItem = parseDetailMenuItem(menu.getSoupDisplay());
        MealPlanDetailResponse.MenuItem main1Item = parseDetailMenuItem(menu.getMain1Display());
        MealPlanDetailResponse.MenuItem main2Item = parseDetailMenuItem(menu.getMain2Display());
        MealPlanDetailResponse.MenuItem sideItem = parseDetailMenuItem(menu.getSideDisplay());
        MealPlanDetailResponse.MenuItem kimchiItem = parseDetailMenuItem(menu.getKimchiDisplay());
        MealPlanDetailResponse.MenuItem dessertItem = parseDetailMenuItem(menu.getDessertDisplay());

        MealPlanDetailResponse.MenuItems menuItems = MealPlanDetailResponse.MenuItems.builder()
                .rice(riceItem)
                .soup(soupItem)
                .main1(main1Item)
                .main2(main2Item)
                .side(sideItem)
                .kimchi(kimchiItem)
                .dessert(dessertItem)
                .build();

        MealPlanDetailResponse.Nutrition nutrition = MealPlanDetailResponse.Nutrition.builder()
                .kcal(menu.getKcal() != null ? menu.getKcal().intValue() : 0)
                .carb(menu.getCarb() != null ? menu.getCarb().intValue() : 0)
                .prot(menu.getProt() != null ? menu.getProt().intValue() : 0)
                .fat(menu.getFat() != null ? menu.getFat().intValue() : 0)
                .build();

        MealPlanDetailResponse.AllergenSummary allergenSummary = buildDetailAllergenSummary(
                riceItem, soupItem, main1Item, main2Item, sideItem, kimchiItem, dessertItem
        );

        return MealPlanDetailResponse.builder()
                .menuId(menu.getId())
                .mealPlanId(menu.getMealPlan().getId())
                .schoolId(menu.getMealPlan().getSchoolId())
                .date(menu.getMenuDate())
                .mealType(menu.getMealType().name())
                .nutrition(nutrition)
                .cost(menu.getCost())
                .aiComment(menu.getAiComment())
                .menuItems(menuItems)
                .allergenSummary(allergenSummary)
                .createdAt(menu.getCreatedAt())
                .updatedAt(menu.getUpdatedAt())
                .build();
    }

    private MealPlanDetailResponse.MenuItem parseDetailMenuItem(String display) {
        if (display == null || display.isBlank()) {
            return null;
        }

        String name = display;
        List<Integer> allergens = new ArrayList<>();

        Matcher matcher = ALLERGEN_PATTERN.matcher(display.trim());
        if (matcher.matches()) {
            name = matcher.group(1).trim();
            String allergenStr = matcher.group(2);
            for (String s : allergenStr.split(",")) {
                try {
                    allergens.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        String foodCode = null;
        Optional<FoodInfo> foodOpt = foodInfoRepository.findByFoodNameIgnoreSpace(name);
        if (foodOpt.isPresent()) {
            foodCode = foodOpt.get().getFoodCode();
        }

        return MealPlanDetailResponse.MenuItem.builder()
                .id(foodCode)
                .name(name)
                .display(display)
                .allergens(allergens)
                .build();
    }

    private MealPlanDetailResponse.AllergenSummary buildDetailAllergenSummary(
            MealPlanDetailResponse.MenuItem... items
    ) {
        Set<Integer> uniqueAllergens = new TreeSet<>();
        Map<String, List<Integer>> byMenu = new LinkedHashMap<>();

        for (MealPlanDetailResponse.MenuItem item : items) {
            if (item != null && item.getAllergens() != null && !item.getAllergens().isEmpty()) {
                uniqueAllergens.addAll(item.getAllergens());
                byMenu.put(item.getName(), item.getAllergens());
            }
        }

        return MealPlanDetailResponse.AllergenSummary.builder()
                .uniqueAllergens(new ArrayList<>(uniqueAllergens))
                .byMenu(byMenu)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MealPlanMenu> findWeeklyMenus(Long schoolId, LocalDate weekStart, LocalDate weekEnd) {
        List<MealPlanMenu> menus = mealPlanMenuRepository.findByMealPlan_SchoolIdAndMenuDateBetweenOrderByMenuDateAscMealTypeAsc(
                schoolId, weekStart, weekEnd
        );

        menus.sort(Comparator
                .comparing(MealPlanMenu::getMenuDate)
                .thenComparing(m -> m.getMealType().ordinal()));

        return menus;
    }

    public MealPlanWeeklyResponse toWeeklyResponse(Long schoolId, LocalDate weekStart, LocalDate weekEnd,
                                                   Integer currentOffset, List<MealPlanMenu> menus) {
        List<MealPlanWeeklyResponse.WeeklyMenu> weeklyMenus = menus.stream()
                .map(this::toWeeklyMenu)
                .collect(Collectors.toList());

        return MealPlanWeeklyResponse.builder()
                .schoolId(schoolId)
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .prevWeekStart(weekStart.minusWeeks(1))
                .nextWeekStart(weekStart.plusWeeks(1))
                .currentOffset(currentOffset)
                .menus(weeklyMenus)
                .build();
    }

    private MealPlanWeeklyResponse.WeeklyMenu toWeeklyMenu(MealPlanMenu menu) {
        List<String> rawMenus = new ArrayList<>();
        Map<String, List<Integer>> byMenu = new LinkedHashMap<>();
        Set<Integer> uniqueAllergens = new TreeSet<>();

        processMenuItemForWeekly(menu.getRiceDisplay(), rawMenus, byMenu, uniqueAllergens);
        processMenuItemForWeekly(menu.getSoupDisplay(), rawMenus, byMenu, uniqueAllergens);
        processMenuItemForWeekly(menu.getMain1Display(), rawMenus, byMenu, uniqueAllergens);
        processMenuItemForWeekly(menu.getMain2Display(), rawMenus, byMenu, uniqueAllergens);
        processMenuItemForWeekly(menu.getSideDisplay(), rawMenus, byMenu, uniqueAllergens);
        processMenuItemForWeekly(menu.getKimchiDisplay(), rawMenus, byMenu, uniqueAllergens);
        processMenuItemForWeekly(menu.getDessertDisplay(), rawMenus, byMenu, uniqueAllergens);

        MealPlanWeeklyResponse.AllergenSummary allergenSummary = MealPlanWeeklyResponse.AllergenSummary.builder()
                .uniqueAllergens(new ArrayList<>(uniqueAllergens))
                .byMenu(byMenu)
                .build();

        return MealPlanWeeklyResponse.WeeklyMenu.builder()
                .id(menu.getId())
                .date(menu.getMenuDate())
                .mealType(menu.getMealType().name())
                .rawMenus(rawMenus)
                .allergenSummary(allergenSummary)
                .build();
    }

    private void processMenuItemForWeekly(String display, List<String> rawMenus,
                                          Map<String, List<Integer>> byMenu, Set<Integer> uniqueAllergens) {
        if (display == null || display.isBlank()) {
            return;
        }

        String name = display;
        List<Integer> allergens = new ArrayList<>();

        Matcher matcher = ALLERGEN_PATTERN.matcher(display.trim());
        if (matcher.matches()) {
            name = matcher.group(1).trim();
            String allergenStr = matcher.group(2);
            for (String s : allergenStr.split(",")) {
                try {
                    allergens.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        rawMenus.add(name);
        byMenu.put(name, allergens);
        uniqueAllergens.addAll(allergens);
    }

    // =========================================================================
    // 8. [AI 대체] 1끼 AI 자동 대체 (RestClient 사용)
    // =========================================================================
    @Transactional
    public MealPlanAIReplaceResponse replaceMenuWithAi(Long schoolId, LocalDate date, MealType mealType) {
        log.info("🤖 AI 자동 대체 요청: schoolId={}, date={}, mealType={}", schoolId, date, mealType);

        MealPlanMenu menu = mealPlanMenuRepository.findByMealPlan_SchoolIdAndMenuDateAndMealType(
                schoolId, date, mealType
        ).orElseThrow(() -> new IllegalArgumentException("해당 날짜의 식단표를 찾을 수 없습니다."));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("date", date.toString());
        requestBody.put("meal_type", mealType.name());
        requestBody.put("school_id", schoolId);

        Map<String, String> currentMenus = new HashMap<>();
        currentMenus.put("rice", menu.getRiceDisplay());
        currentMenus.put("soup", menu.getSoupDisplay());
        currentMenus.put("main1", menu.getMain1Display());
        currentMenus.put("main2", menu.getMain2Display());
        currentMenus.put("side", menu.getSideDisplay());
        currentMenus.put("kimchi", menu.getKimchiDisplay());
        currentMenus.put("dessert", menu.getDessertDisplay());
        requestBody.put("current_menus", currentMenus);

        String oldMenus = buildMenuString(menu);

        log.info("🚀 FastAPI AI 대체 호출: /v1/menus/single:generate");

        JsonNode result;
        try {
            result = restClient.post()
                    .uri("/v1/menus/single:generate")
                    .headers(httpHeaders -> httpHeaders.addAll(createHeaders()))
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.error("FastAPI AI 대체 호출 실패", e);
            throw new RuntimeException("AI 대체 서비스 오류");
        }

        log.info("✅ FastAPI AI 응답 수신: {}", result.toString());

        if (result.has("menus") && result.get("menus").isArray()) {
            JsonNode menusArray = result.get("menus");
            if (menusArray.size() > 0) menu.setRiceDisplay(enrichWithAllergen(menusArray.get(0).asText(null)));
            if (menusArray.size() > 1) menu.setSoupDisplay(enrichWithAllergen(menusArray.get(1).asText(null)));
            if (menusArray.size() > 2) menu.setMain1Display(enrichWithAllergen(menusArray.get(2).asText(null)));
            if (menusArray.size() > 3) menu.setMain2Display(enrichWithAllergen(menusArray.get(3).asText(null)));
            if (menusArray.size() > 4) menu.setSideDisplay(enrichWithAllergen(menusArray.get(4).asText(null)));
            if (menusArray.size() > 5) menu.setKimchiDisplay(enrichWithAllergen(menusArray.get(5).asText(null)));
            if (menusArray.size() > 6) menu.setDessertDisplay(enrichWithAllergen(menusArray.get(6).asText(null)));
        }

        if (result.has("kcal")) menu.setKcal(BigDecimal.valueOf(result.get("kcal").asDouble()));
        else if (result.has("Kcal")) menu.setKcal(BigDecimal.valueOf(result.get("Kcal").asDouble()));

        if (result.has("carb")) menu.setCarb(BigDecimal.valueOf(result.get("carb").asDouble()));
        else if (result.has("Carb")) menu.setCarb(BigDecimal.valueOf(result.get("Carb").asDouble()));

        if (result.has("prot")) menu.setProt(BigDecimal.valueOf(result.get("prot").asDouble()));
        else if (result.has("Prot")) menu.setProt(BigDecimal.valueOf(result.get("Prot").asDouble()));

        if (result.has("fat")) menu.setFat(BigDecimal.valueOf(result.get("fat").asDouble()));
        else if (result.has("Fat")) menu.setFat(BigDecimal.valueOf(result.get("Fat").asDouble()));

        if (result.has("cost")) menu.setCost(result.get("cost").asInt());
        else if (result.has("Cost")) menu.setCost(result.get("Cost").asInt());

        String aiComment = result.has("reason") ? result.get("reason").asText() : "AI 자동 대체";
        menu.setAiComment(aiComment);

        MealPlanMenu savedMenu = mealPlanMenuRepository.save(menu);
        log.info("✅ AI 대체 완료: menuId={}", savedMenu.getId());

        String newMenus = buildMenuString(savedMenu);

        saveHistory(
                date.toString(),
                mealType.name(),
                oldMenus,
                newMenus,
                aiComment,
                MenuHistory.ActionType.AI_AUTO_REPLACE,
                menu.getCreatedAt()
        );

        return MealPlanAIReplaceResponse.builder()
                .mealPlanId(savedMenu.getMealPlan().getId())
                .menuId(savedMenu.getId())
                .date(savedMenu.getMenuDate())
                .mealType(savedMenu.getMealType().name())
                .replaced(true)
                .aiComment(aiComment)
                .updatedAt(savedMenu.getUpdatedAt())
                .build();
    }

    // =========================================================================
    // 9. [수동 수정] & 10. [히스토리 조회]
    // =========================================================================

    @Transactional
    public MealPlanManualUpdateResponse updateMenuManually(Long mealPlanId, Long menuId, List<String> newMenus, String reason) {
        log.info("✏️ 식단표 수동 수정 요청: mealPlanId={}, menuId={}", mealPlanId, menuId);

        MealPlanMenu menu = mealPlanMenuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("해당 식단표를 찾을 수 없습니다."));

        if (!menu.getMealPlan().getId().equals(mealPlanId)) {
            throw new IllegalArgumentException("해당 식단표를 찾을 수 없습니다.");
        }

        String oldMenus = buildMenuString(menu);

        List<String> rawMenus = new ArrayList<>();
        List<String> displayMenus = new ArrayList<>();
        Map<String, List<Integer>> byMenu = new LinkedHashMap<>();
        Set<Integer> uniqueAllergens = new TreeSet<>();

        BigDecimal totalKcal = BigDecimal.ZERO;
        BigDecimal totalCarb = BigDecimal.ZERO;
        BigDecimal totalProt = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;

        for (String menuName : newMenus) {
            String pureName = menuName.replaceAll("\\s*\\([^)]*\\)", "").trim();
            if (pureName.isEmpty()) continue;

            rawMenus.add(pureName);

            Optional<FoodInfo> foodOpt = foodInfoRepository.findByFoodNameIgnoreSpace(pureName);

            if (foodOpt.isPresent()) {
                FoodInfo food = foodOpt.get();

                List<Integer> allergens = new ArrayList<>();
                String allergyDisplay = "";
                if (food.getAllergyInfo() != null && !food.getAllergyInfo().isEmpty()) {
                    allergyDisplay = "(" + food.getAllergyInfo() + ")";
                    for (String s : food.getAllergyInfo().split(",")) {
                        try {
                            allergens.add(Integer.parseInt(s.trim()));
                        } catch (NumberFormatException ignored) {}
                    }
                }

                displayMenus.add(pureName + allergyDisplay);
                byMenu.put(pureName, allergens);
                uniqueAllergens.addAll(allergens);

                if (food.getKcal() != null) totalKcal = totalKcal.add(BigDecimal.valueOf(food.getKcal()));
                if (food.getCarbs() != null) totalCarb = totalCarb.add(food.getCarbs());
                if (food.getProtein() != null) totalProt = totalProt.add(food.getProtein());
                if (food.getFat() != null) totalFat = totalFat.add(food.getFat());
            } else {
                displayMenus.add(pureName);
                byMenu.put(pureName, new ArrayList<>());
            }
        }

        menu.setRiceDisplay(displayMenus.size() > 0 ? displayMenus.get(0) : null);
        menu.setSoupDisplay(displayMenus.size() > 1 ? displayMenus.get(1) : null);
        menu.setMain1Display(displayMenus.size() > 2 ? displayMenus.get(2) : null);
        menu.setMain2Display(displayMenus.size() > 3 ? displayMenus.get(3) : null);
        menu.setSideDisplay(displayMenus.size() > 4 ? displayMenus.get(4) : null);
        menu.setKimchiDisplay(displayMenus.size() > 5 ? displayMenus.get(5) : null);
        menu.setDessertDisplay(displayMenus.size() > 6 ? displayMenus.get(6) : null);

        menu.setKcal(totalKcal);
        menu.setCarb(totalCarb);
        menu.setProt(totalProt);
        menu.setFat(totalFat);

        try {
            menu.setRawMenusJson(objectMapper.writeValueAsString(rawMenus));
        } catch (Exception e) {
            log.warn("⚠️ rawMenusJson 변환 실패: {}", e.getMessage());
            menu.setRawMenusJson(rawMenus.toString());
        }

        menu.setAiComment(reason);

        MealPlanMenu savedMenu = mealPlanMenuRepository.save(menu);
        log.info("✅ 수동 수정 완료: menuId={}", savedMenu.getId());

        saveHistory(
                savedMenu.getMenuDate().toString(),
                savedMenu.getMealType().name(),
                oldMenus,
                String.join(MENU_DELIMITER, displayMenus),
                reason,
                MenuHistory.ActionType.MANUAL_UPDATE,
                menu.getCreatedAt()
        );

        MealPlanManualUpdateResponse.AllergenSummary allergenSummary = MealPlanManualUpdateResponse.AllergenSummary.builder()
                .uniqueAllergens(new ArrayList<>(uniqueAllergens))
                .byMenu(byMenu)
                .build();

        return MealPlanManualUpdateResponse.builder()
                .menuId(savedMenu.getId())
                .mealPlanId(savedMenu.getMealPlan().getId())
                .date(savedMenu.getMenuDate())
                .mealType(savedMenu.getMealType().name())
                .reason(reason)
                .rawMenus(rawMenus)
                .allergenSummary(allergenSummary)
                .updatedAt(savedMenu.getUpdatedAt())
                .build();
    }

    private static final String MENU_DELIMITER = ", ";

    private String enrichWithAllergen(String menuName) {
        if (menuName == null || menuName.isBlank()) {
            return null;
        }

        if (menuName.matches(".*\\([\\d,\\s]+\\)$")) {
            return menuName;
        }

        String pureName = menuName.replaceAll("\\s*\\([^)]*\\)", "").trim();

        Optional<FoodInfo> foodOpt = foodInfoRepository.findByFoodNameIgnoreSpace(pureName);
        if (foodOpt.isPresent()) {
            FoodInfo food = foodOpt.get();
            if (food.getAllergyInfo() != null && !food.getAllergyInfo().isEmpty()) {
                return pureName + " (" + food.getAllergyInfo() + ")";
            }
        }

        return pureName;
    }

    private String buildMenuString(MealPlanMenu menu) {
        List<String> menus = new ArrayList<>();
        if (menu.getRiceDisplay() != null) menus.add(menu.getRiceDisplay());
        if (menu.getSoupDisplay() != null) menus.add(menu.getSoupDisplay());
        if (menu.getMain1Display() != null) menus.add(menu.getMain1Display());
        if (menu.getMain2Display() != null) menus.add(menu.getMain2Display());
        if (menu.getSideDisplay() != null) menus.add(menu.getSideDisplay());
        if (menu.getKimchiDisplay() != null) menus.add(menu.getKimchiDisplay());
        if (menu.getDessertDisplay() != null) menus.add(menu.getDessertDisplay());
        return String.join(MENU_DELIMITER, menus);
    }

    @Transactional(readOnly = true)
    public MealPlanHistoryResponse getHistories(String startDate, String endDate, String actionType, int page, int size) {
        log.info("📜 히스토리 조회: startDate={}, endDate={}, actionType={}, page={}, size={}",
                startDate, endDate, actionType, page, size);

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MenuHistory> historyPage;

        MenuHistory.ActionType actionTypeEnum = null;
        boolean isAllActionType = false;

        if (actionType != null && !actionType.isBlank()) {
            String upperActionType = actionType.toUpperCase();
            if ("ALL".equals(upperActionType)) {
                isAllActionType = true;
            } else {
                try {
                    actionTypeEnum = MenuHistory.ActionType.valueOf(upperActionType);
                } catch (IllegalArgumentException e) {
                    log.warn("⚠️ 유효하지 않은 actionType: {}", actionType);
                }
            }
        }

        boolean hasDateRange = startDate != null && !startDate.isBlank() && endDate != null && !endDate.isBlank();
        boolean hasActionType = actionTypeEnum != null && !isAllActionType;

        if (hasDateRange && hasActionType) {
            historyPage = menuHistoryRepository.findByMealDateBetweenAndActionTypeOrderByIdDesc(
                    startDate, endDate, actionTypeEnum, pageRequest);
        } else if (hasDateRange) {
            historyPage = menuHistoryRepository.findByMealDateBetweenOrderByIdDesc(
                    startDate, endDate, pageRequest);
        } else if (hasActionType) {
            historyPage = menuHistoryRepository.findByActionTypeOrderByIdDesc(actionTypeEnum, pageRequest);
        } else {
            historyPage = menuHistoryRepository.findAllByOrderByIdDesc(pageRequest);
        }

        List<MealPlanHistoryResponse.HistoryItem> items = historyPage.getContent().stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());

        return MealPlanHistoryResponse.builder()
                .currentPage(page)
                .pageSize(size)
                .totalItems(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .items(items)
                .build();
    }

    private MealPlanHistoryResponse.HistoryItem toHistoryItem(MenuHistory history) {
        return MealPlanHistoryResponse.HistoryItem.builder()
                .id(history.getId())
                .mealDate(history.getMealDate())
                .mealType(history.getMealType())
                .actionType(history.getActionType().name())
                .oldMenus(parseMenuString(history.getOldMenus()))
                .newMenus(parseMenuString(history.getNewMenus()))
                .reason(history.getReason())
                .menuCreatedAt(history.getMenuCreatedAt())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private List<String> parseMenuString(String menuString) {
        if (menuString == null || menuString.isBlank()) {
            return new ArrayList<>();
        }

        if (menuString.contains(" || ")) {
            return Arrays.stream(menuString.split("\\s*\\|\\|\\s*"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;

        for (int i = 0; i < menuString.length(); i++) {
            char c = menuString.charAt(i);
            if (c == '(') {
                parenDepth++;
                current.append(c);
            } else if (c == ')') {
                parenDepth--;
                current.append(c);
            } else if (c == ',' && parenDepth == 0) {
                String item = current.toString().trim();
                if (!item.isEmpty()) {
                    result.add(item);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String lastItem = current.toString().trim();
        if (!lastItem.isEmpty()) {
            result.add(lastItem);
        }

        return result;
    }
}