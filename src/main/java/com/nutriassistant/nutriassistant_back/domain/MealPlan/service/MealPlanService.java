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
import com.nutriassistant.nutriassistant_back.domain.ai.service.ImageGenerationService;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.entity.MonthlyOpsDoc;
import com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.service.MonthlyOpsDocService;
import com.nutriassistant.nutriassistant_back.domain.review.repository.ReviewRepository;
// [추가] S3 업로더 import
import com.nutriassistant.nutriassistant_back.global.aws.S3Uploader;

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

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final FoodInfoRepository foodInfoRepository;
    private final NewFoodInfoRepository newFoodInfoRepository;
    private final MonthlyOpsDocService monthlyOpsDocService;
    private final ImageGenerationService imageGenerationService;
    private final ReviewRepository reviewRepository;

    // [추가] S3 업로더 주입
    private final S3Uploader s3Uploader;

    // --- 환경 변수 ---
    @Value("${fastapi.base-url:http://localhost:8001}")
    private String fastApiBaseUrl;

    @Value("${fastapi.internal-token:}")
    private String internalToken;

    public MealPlanService(MealPlanRepository mealPlanRepository,
                           MealPlanMenuRepository mealPlanMenuRepository,
                           MenuHistoryRepository menuHistoryRepository,
                           MealPlanMenuService mealPlanMenuService,
                           MonthlyOpsDocService monthlyOpsDocService,
                           RestClient restClient,
                           ObjectMapper objectMapper,
                           FoodInfoRepository foodInfoRepository,
                           NewFoodInfoRepository newFoodInfoRepository,
                           ImageGenerationService imageGenerationService,
                           ReviewRepository reviewRepository,
                           S3Uploader s3Uploader // [추가] 생성자 주입
    ) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanMenuRepository = mealPlanMenuRepository;
        this.menuHistoryRepository = menuHistoryRepository;
        this.mealPlanMenuService = mealPlanMenuService;
        this.monthlyOpsDocService = monthlyOpsDocService;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.foodInfoRepository = foodInfoRepository;
        this.newFoodInfoRepository = newFoodInfoRepository;
        this.imageGenerationService = imageGenerationService;
        this.reviewRepository = reviewRepository;
        this.s3Uploader = s3Uploader; // [할당]
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
    // [수정] 메인 화면용: 오늘의 식단 조회 (이미지 생성 -> S3 업로드 -> URL 저장)
    // =========================================================================
    @Transactional
    public MealPlanDetailResponse getTodayMealPlan(Long schoolId, Long studentId) {
        LocalDate today = LocalDate.now();

        /* 1. 오늘의 점심 식단 조회 (학교 ID, 날짜, 식사 타입을 기준으로 조회) */
        MealPlanMenu menu = mealPlanMenuRepository.findByMealPlan_SchoolIdAndMenuDateAndMealType(
                schoolId, today, MealType.LUNCH
        ).orElseThrow(() -> new IllegalArgumentException("오늘의 중식 식단이 존재하지 않습니다."));

        /* 2. 메뉴 엔티티와 연관된 부모 식단(MealPlan) 정보 획득 */
        MealPlan mealPlan = menu.getMealPlan();

        /* 3. 저장된 이미지 URL이 없는 경우 AI를 통해 이미지를 생성하고 S3에 저장함 */
        if (mealPlan.getImageUrl() == null || mealPlan.getImageUrl().isBlank()) {
            try {
                /* 3-1. 현재 식단 메뉴에서 음식 이름 리스트만 추출함 */
                List<String> menuNames = extractMenuNames(menu);

                if (!menuNames.isEmpty()) {
                    log.info("오늘의 식단 이미지 생성 시작: {}", menuNames);

                    /* * 3-2. AI 이미지 생성 및 S3 업로드 통합 서비스 호출
                     * ImageGenerationService 내부의 generateAndSaveMealImage 메서드가
                     * 이미지 생성, Base64 디코딩, S3 업로드를 모두 처리하고 최종 URL을 반환함
                     */
                    String s3Url = imageGenerationService.generateAndSaveMealImage(menuNames);

                    /* 3-3. 생성된 S3 URL을 식단 엔티티에 업데이트하여 DB에 반영함 */
                    mealPlan.updateImageUrl(s3Url);
                    log.info("식단 이미지 생성 및 S3 업로드 완료: {}", s3Url);
                }
            } catch (Exception e) {
                /* AI 서비스 장애가 발생하더라도 사용자에게는 식단 텍스트 정보라도 보여주기 위해 예외를 로그로만 남김 */
                log.error("AI 이미지 생성 또는 S3 업로드 실패 (상세 로그): ", e);
            }
        }

        // 4. 리뷰 작성 여부 확인
        boolean isReviewed = false;
        if (studentId != null) {
            isReviewed = reviewRepository.existsByStudentIdAndDateAndMealType(
                    studentId, menu.getMenuDate(), MealType.valueOf(String.valueOf(menu.getMealType()))
            );
        }

        // 5. 응답 DTO 변환 (리뷰 여부 전달)
        return toDetailResponse(menu, isReviewed);
    }

    // [헬퍼] 메뉴 객체에서 음식 이름만 리스트로 추출
    private List<String> extractMenuNames(MealPlanMenu menu) {
        List<String> names = new ArrayList<>();
        addIfPresent(names, menu.getRiceDisplay());
        addIfPresent(names, menu.getSoupDisplay());
        addIfPresent(names, menu.getMain1Display());
        addIfPresent(names, menu.getMain2Display());
        addIfPresent(names, menu.getSideDisplay());
        addIfPresent(names, menu.getKimchiDisplay());
        addIfPresent(names, menu.getDessertDisplay());
        return names;
    }

    private void addIfPresent(List<String> list, String display) {
        if (display != null && !display.isBlank()) {
            String pureName = display.replaceAll("\\s*\\([^)]*\\)", "").trim();
            list.add(pureName);
        }
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

        // 운영 일지 조회
        JsonNode reportData = null;
        int reportYear = year;
        int reportMonth = month - 1;
        if (reportMonth == 0) {
            reportMonth = 12;
            reportYear -= 1;
        }

        log.info("📊 운영 일지(MonthlyOpsDoc) 조회 시도: {}년 {}월", reportYear, reportMonth);

        Optional<MonthlyOpsDoc> docOpt = monthlyOpsDocService.findByYearAndMonth(
                reportYear, reportMonth
        );

        if (docOpt.isPresent()) {
            reportData = monthlyOpsDocService.getReportDataAsJson(docOpt.get());
            log.info("✅ 운영 일지 발견 → FastAPI로 전달");
        } else {
            log.info("ℹ️ 운영 일지 없음 → 기본 가중치로 식단 생성");
        }

        // FastAPI 요청 Body 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("year", year);
        requestBody.put("month", month);
        requestBody.put("school_id", schoolId);

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

        if (reportData != null) {
            requestBody.put("report", objectMapper.convertValue(reportData, Map.class));
        }

        // 신메뉴 추가
        List<NewFoodInfo> newFoodInfoList = newFoodInfoRepository.findBySchoolIdAndDeletedFalse(schoolId);
        if (!newFoodInfoList.isEmpty()) {
            List<Map<String, Object>> newMenus = newFoodInfoList.stream()
                    .map(this::convertNewFoodInfoToMap)
                    .collect(Collectors.toList());
            requestBody.put("new_menus", newMenus);
            log.info("📋 신메뉴 {}개 추가", newMenus.size());
        }

        // FastAPI 호출
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

        // DB 저장
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
    private void saveHistory(Long schoolId, String date, String type, String oldM, String newM, String reason,
                             MenuHistory.ActionType action, LocalDateTime menuCreatedAt) {
        MenuHistory history = MenuHistory.builder()
                .schoolId(schoolId)
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
            headers.set("X-Internal-API-Key", internalToken);
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

    // =========================================================================
    // [수정] MealPlanDetailResponse 변환 (리뷰 여부 포함)
    // =========================================================================

    // 1. 기존 호환성 유지용 (리뷰 여부 false)
    public MealPlanDetailResponse toDetailResponse(MealPlanMenu menu) {
        return toDetailResponse(menu, false);
    }

    // 2. 리뷰 여부 포함 버전
    public MealPlanDetailResponse toDetailResponse(MealPlanMenu menu, boolean isReviewed) {
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

                // ▼▼▼ [추가] 이미지 URL 및 리뷰 여부 매핑 ▼▼▼
                .imageUrl(menu.getMealPlan().getImageUrl())
                .isReviewed(isReviewed)

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
                schoolId,
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
    public MealPlanManualUpdateResponse updateMenuManually(Long schoolId, Long mealPlanId, Long menuId, List<String> newMenus, String reason) {
        log.info("✏️ 식단표 수동 수정 요청: schoolId={}, mealPlanId={}, menuId={}", schoolId, mealPlanId, menuId);

        MealPlanMenu menu = mealPlanMenuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("해당 식단표를 찾을 수 없습니다."));

        if (!menu.getMealPlan().getId().equals(mealPlanId)) {
            throw new IllegalArgumentException("해당 식단표를 찾을 수 없습니다.");
        }

        // schoolId 검증: 본인 학교 식단표만 수정 가능
        if (!menu.getMealPlan().getSchoolId().equals(schoolId)) {
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
                schoolId,
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
    public MealPlanHistoryResponse getHistories(Long schoolId, String startDate, String endDate, String actionType, int page, int size) {
        log.info("📜 히스토리 조회: schoolId={}, startDate={}, endDate={}, actionType={}, page={}, size={}",
                schoolId, startDate, endDate, actionType, page, size);

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
            historyPage = menuHistoryRepository.findBySchoolIdAndMealDateBetweenAndActionTypeOrderByIdDesc(
                    schoolId, startDate, endDate, actionTypeEnum, pageRequest);
        } else if (hasDateRange) {
            historyPage = menuHistoryRepository.findBySchoolIdAndMealDateBetweenOrderByIdDesc(
                    schoolId, startDate, endDate, pageRequest);
        } else if (hasActionType) {
            historyPage = menuHistoryRepository.findBySchoolIdAndActionTypeOrderByIdDesc(schoolId, actionTypeEnum, pageRequest);
        } else {
            historyPage = menuHistoryRepository.findBySchoolIdOrderByIdDesc(schoolId, pageRequest);
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