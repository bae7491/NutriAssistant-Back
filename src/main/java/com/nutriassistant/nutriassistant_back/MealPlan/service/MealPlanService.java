package com.nutriassistant.nutriassistant_back.MealPlan.service;

import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanAIReplaceResponse;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanCreateRequest;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanDetailResponse;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanGenerateRequest;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanGenerateResponse;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanHistoryResponse;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanManualUpdateResponse;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanMonthlyResponse;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanWeeklyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.nutriassistant.nutriassistant_back.MealPlan.entity.*;
import com.nutriassistant.nutriassistant_back.MealPlan.entity.*;
import com.nutriassistant.nutriassistant_back.MealPlan.repository.FoodInfoRepository;
import com.nutriassistant.nutriassistant_back.MealPlan.repository.MealPlanMenuRepository;
import com.nutriassistant.nutriassistant_back.MealPlan.repository.MealPlanRepository;
import com.nutriassistant.nutriassistant_back.MealPlan.repository.MenuHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;
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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FoodInfoRepository foodInfoRepository;
    private final ReportService reportService;


    // --- 환경 변수 (application.yml) ---
    @Value("${fastapi.base-url:http://localhost:8001}")
    private String fastApiBaseUrl;

    @Value("${fastapi.internal-token:}")
    private String internalToken;

    public MealPlanService(MealPlanRepository mealPlanRepository,
                           MealPlanMenuRepository mealPlanMenuRepository,
                           MenuHistoryRepository menuHistoryRepository,
                           MealPlanMenuService mealPlanMenuService,
                           ReportService reportService,
                           RestTemplate restTemplate,
                           ObjectMapper objectMapper,
                           FoodInfoRepository foodInfoRepository
                           ) {
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanMenuRepository = mealPlanMenuRepository;
        this.menuHistoryRepository = menuHistoryRepository;
        this.mealPlanMenuService = mealPlanMenuService;
        this.reportService = reportService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.foodInfoRepository = foodInfoRepository;
    }

    // =========================================================================
    // [조회] 월간 식단표 조회
    // =========================================================================
    @Transactional(readOnly = true)
    public MealPlan getById(Long mealPlanId) {
        return mealPlanRepository.findById(mealPlanId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 식단 계획을 찾을 수 없습니다. ID=" + mealPlanId));
    }

    @Transactional(readOnly = true)
    public Optional<MealPlan> findById(Long mealPlanId) {
        return mealPlanRepository.findById(mealPlanId);
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
        // 1. DB에서 이전 달 리포트 조회
        // ========================================
        JsonNode reportData = null;

        // 이전 달 계산
        int reportYear = year;
        int reportMonth = month - 1;
        if (reportMonth == 0) {
            reportMonth = 12;
            reportYear -= 1;
        }

        log.info("📊 리포트 조회 시도: {}년 {}월", reportYear, reportMonth);

        Optional<Report> reportOpt = reportService.findByYearAndMonth(
                reportYear, reportMonth
        );

        if (reportOpt.isPresent()) {
            reportData = reportService.getReportDataAsJson(reportOpt.get());
            log.info("✅ 리포트 발견 → FastAPI로 전달 (가중치 분석 예정)");
        } else {
            log.info("ℹ️ 리포트 없음 → 기본 가중치로 식단 생성");
        }

        // ========================================
        // 2. FastAPI 요청 Body 구성
        // ========================================
        String url = String.format("%s/month/generate", fastApiBaseUrl);

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

                if (c.getNutritionKey() != null) {
                    constraints.put("nutrition_key", c.getNutritionKey());
                }
                if (c.getTargetPrice() != null) {
                    constraints.put("target_price", c.getTargetPrice());
                }
                if (c.getMaxPriceLimit() != null) {
                    constraints.put("max_price_limit", c.getMaxPriceLimit());
                }
                if (c.getCookStaff() != null) {
                    constraints.put("cook_staff", c.getCookStaff());
                }
                if (c.getFacilityText() != null) {
                    constraints.put("facility_text", c.getFacilityText());
                }

                options.put("constraints", constraints);
            }

            requestBody.put("options", options);
        }

        // DB에서 조회한 리포트 추가
        if (reportData != null) {
            requestBody.put("report", objectMapper.convertValue(reportData, Map.class));
        }

        // ========================================
        // 3. FastAPI 호출
        // ========================================
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("🚀 FastAPI 호출: {}", url);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        JsonNode fastPayload = Objects.requireNonNull(response.getBody());

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
    // [저장 로직] 식단 데이터 저장 및 갱신
    // =========================================================================
    @Transactional
    public MealPlan createOrReplace(MealPlanCreateRequest req) {
        log.info("💾 DB 저장: {}년 {}월 (학교 ID: {})", req.year(), req.month(), req.schoolId());

        // schoolId, year, month로 조회
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

        // FastAPI 응답 구조에 맞춰 payload 구성
        JsonNode payload = objectMapper.createObjectNode()
                .set("meals", req.menus());

        log.info("💾 메뉴 저장 시작...");
        mealPlanMenuService.importFromFastApi(savedPlan.getId(), payload);
        log.info("✅ 메뉴 저장 완료");

        return savedPlan;
    }
//
//    // =========================================================================
//    // 2. [AI 수정] 원클릭 메뉴 대체 (1끼)
//    // =========================================================================
//    @Transactional
//    public void replaceMenuWithAi(String dateStr, String mealTypeStr) {
//        LocalDate date = LocalDate.parse(dateStr);
//        MealType mealType = MealType.valueOf(mealTypeStr);
//
//        // FastAPI 요청
//        String url = String.format("%s/v1/menus/single:generate", fastApiBaseUrl);
//        HttpHeaders headers = createHeaders();
//        Map<String, String> body = Map.of("date", dateStr, "meal_type", mealTypeStr);
//        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
//
//        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
//        Map<String, Object> result = response.getBody();
//
//        // 8개 후보군 검증
//        System.out.println("\n🕵️ [AI 로직 검증] 8개 후보군 생성 여부 확인");
//        if (result != null) {
//            if (result.containsKey("candidates")) {
//                List<?> candidates = (List<?>) result.get("candidates");
//                int count = candidates.size();
//                System.out.println("✅ 'candidates' 데이터 발견됨!");
//                System.out.println("📊 생성된 후보 개수: " + count + "개");
//                if (count == 8) {
//                    System.out.println("🎉 검증 성공: 8개의 후보 중에서 최적의 식단이 선택되었습니다.");
//                } else {
//                    System.out.println("⚠️ 검증 경고: 후보 개수가 8개가 아닙니다 (" + count + "개).");
//                }
//            } else {
//                System.out.println("⚠️ 'candidates' 키가 없습니다.");
//            }
//        }
//        System.out.println("--------------------------------------------------\n");
//
//        // Python 응답 디버깅
//        System.out.println("=== Python AI 응답 ===");
//        System.out.println(result);
//        System.out.println("menus: " + result.get("menus"));
//        System.out.println("rawMenus: " + result.get("rawMenus"));
//        System.out.println("dessert: " + result.get("dessert"));
//        System.out.println("kcal: " + result.get("kcal"));
//        System.out.println("carb: " + result.get("carb"));
//        System.out.println("prot: " + result.get("prot"));
//        System.out.println("fat: " + result.get("fat"));
//        System.out.println("cost: " + result.get("cost"));
//        System.out.println("====================");
//
//        MealPlanMenu menu = mealPlanMenuRepository.findByDateAndType(date, mealType)
//                .orElseThrow(() -> new IllegalArgumentException("수정할 식단 데이터가 없습니다."));
//
//        String oldMenus = menu.getMenuString();
//
//        List<String> newMenus = (List<String>) result.get("menus");
//        List<String> rawMenus = (List<String>) result.get("rawMenus");
//        String aiReason = (String) result.get("reason");
//
//        // ★★★ 모든 정보를 한 번에 업데이트 ★★★
//        // 1. 메뉴 정보 업데이트
//        menu.updateMenus(newMenus);
//        menu.updateRawMenus(rawMenus, objectMapper);
//        menu.setAiComment(aiReason);
//
//        // 2. 영양 정보 업데이트
//        if (result.get("kcal") != null) {
//            Double kcalValue = Double.valueOf(result.get("kcal").toString());
//            System.out.println("🔄 kcal 업데이트: " + menu.getKcal() + " -> " + kcalValue);
//            menu.setKcal(kcalValue);
//        }
//        if (result.get("carb") != null) {
//            Double carbValue = Double.valueOf(result.get("carb").toString());
//            System.out.println("🔄 carb 업데이트: " + menu.getCarb() + " -> " + carbValue);
//            menu.setCarb(carbValue);
//        }
//        if (result.get("prot") != null) {
//            Double protValue = Double.valueOf(result.get("prot").toString());
//            System.out.println("🔄 prot 업데이트: " + menu.getProt() + " -> " + protValue);
//            menu.setProt(protValue);
//        }
//        if (result.get("fat") != null) {
//            Double fatValue = Double.valueOf(result.get("fat").toString());
//            System.out.println("🔄 fat 업데이트: " + menu.getFat() + " -> " + fatValue);
//            menu.setFat(fatValue);
//        }
//
//        // 3. 비용 정보 업데이트
//        if (result.get("cost") != null) {
//            Integer costValue = Integer.valueOf(result.get("cost").toString());
//            System.out.println("🔄 cost 업데이트: " + menu.getCost() + " -> " + costValue);
//            menu.setCost(costValue);
//        }
//
//        // 4. DB 저장 (한 번만!)
//        mealPlanMenuRepository.save(menu);
//
//        // 5. 히스토리 저장 (한 번만!)
//        saveHistory(dateStr, mealTypeStr, oldMenus, newMenus.toString(), aiReason, MenuHistory.ActionType.AI_AUTO_REPLACE);
//
//        System.out.println("✅ 업데이트 완료!");
//    }
//
//    // =========================================================================
//    // 3. [수동 수정] 사용자가 직접 메뉴 입력
//    // =========================================================================
//    @Transactional
//    public void updateMenuManually(String dateStr, String mealTypeStr, List<String> newMenus, String reason) {
//        // 1. 날짜 및 타입 파싱
//        LocalDate date = LocalDate.parse(dateStr);
//        MealType mealType = MealType.valueOf(mealTypeStr);
//
//        // 2. 기존 식단 데이터 조회
//        MealPlanMenu menu = mealPlanMenuRepository.findByDateAndType(date, mealType)
//                .orElseThrow(() -> new IllegalArgumentException("수정할 식단 데이터가 없습니다."));
//
//        String oldMenus = menu.getMenuString();
//
//        // 3. 변수 초기화
//        List<String> finalDisplayMenus = new ArrayList<>();
//        List<String> pureRawMenus = new ArrayList<>();
//
//        // [수정] 영양소 합산용 변수 (계산은 double로 하고 나중에 Entity에 Double로 넣음)
//        int totalKcal = 0;
//        double totalCarb = 0;
//        double totalProt = 0;
//        double totalFat = 0;
//
//        // --- [로직 시작] 입력된 메뉴 리스트 순회 ---
//        for (String inputMenuName : newMenus) {
//            String pureName = inputMenuName.replaceAll("\\s*\\([^)]*\\)", "").trim();
//            if (pureName.isEmpty()) continue;
//            pureRawMenus.add(pureName);
//
//            // (2) 1차 시도: Repository Query로 검색
//            Optional<FoodInfo> foodOpt = foodInfoRepository.findByFoodNameIgnoreSpace(pureName);
//
//            // (3) [비상 대책] 2차 시도: 전수 조사 (DB 쿼리가 실패할 경우 대비)
//            if (foodOpt.isEmpty()) {
//                System.out.println("⚠️ Query 검색 실패: [" + pureName + "] -> 전수 조사 시도");
//                List<FoodInfo> allFoods = foodInfoRepository.findAll();
//
//                for (FoodInfo dbFood : allFoods) {
//                    String dbNameClean = dbFood.getFoodName().replace(" ", "");
//                    String inputNameClean = pureName.replace(" ", "");
//
//                    if (dbNameClean.equals(inputNameClean)) {
//                        System.out.println("✅ [전수 조사 성공] (" + dbFood.getFoodName() + ")");
//                        foodOpt = Optional.of(dbFood);
//                        break;
//                    }
//                }
//            }
//
//            // (4) 데이터 처리
//            if (foodOpt.isPresent()) {
//                FoodInfo food = foodOpt.get();
//
//                // 4-1. 알레르기 정보
//                String allergy = (food.getAllergyInfo() != null && !food.getAllergyInfo().isEmpty())
//                        ? "(" + food.getAllergyInfo() + ")" : "";
//                finalDisplayMenus.add(pureName + allergy);
//
//                // 4-2. 영양 성분 누적 ([수정] BigDecimal -> double 변환 후 누적)
//                totalKcal += (food.getKcal() != null) ? food.getKcal() : 0;
//                totalCarb += (food.getCarbs() != null) ? food.getCarbs().doubleValue() : 0;
//                totalProt += (food.getProtein() != null) ? food.getProtein().doubleValue() : 0;
//                totalFat += (food.getFat() != null) ? food.getFat().doubleValue() : 0;
//
//                System.out.println("🆗 매핑 완료: " + pureName);
//            } else {
//                finalDisplayMenus.add(pureName);
//                System.out.println("❌ 실패: DB에 없음 -> [" + pureName + "]");
//            }
//        }
//
//        // --- [저장 단계] ---
//        try {
//            menu.updateMenus(finalDisplayMenus);
//            String rawJson = objectMapper.writeValueAsString(pureRawMenus);
//            menu.setRawMenusJson(rawJson);
//        } catch (Exception e) {
//            throw new RuntimeException("JSON 변환 오류", e);
//        }
//
//        // [수정] 합산된 영양 정보 저장
//        // Entity(MealPlanMenu)의 필드는 Double 타입입니다.
//        // 따라서 계산된 double 값들을 그대로 넣어주어야 합니다. (int 강제 변환 금지)
//        menu.setKcal((double) totalKcal);
//        menu.setCarb(totalCarb);
//        menu.setProt(totalProt);
//        menu.setFat(totalFat);
//
//        // 기타 정보 저장
//        menu.setAiComment(reason);
//        mealPlanMenuRepository.save(menu);
//
//        saveHistory(dateStr, mealTypeStr, oldMenus, finalDisplayMenus.toString(), reason, MenuHistory.ActionType.MANUAL_UPDATE);
//    }

    // =========================================================================
    // 4. [헬퍼] 공통 내부 메서드
    // =========================================================================
    private void saveHistory(String date, String type, String oldM, String newM, String reason, MenuHistory.ActionType action) {
        MenuHistory history = MenuHistory.builder()
                .mealDate(date)
                .mealType(type)
                .oldMenus(oldM)
                .newMenus(newM)
                .reason(reason)
                .actionType(action)
                .build();
        menuHistoryRepository.save(history);
    }
    
    // HTTP 헤더 생성
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalToken != null && !internalToken.isEmpty()) {
            headers.set("X-Internal-Token", internalToken);
        }
        return headers;
    }

    // =========================================================================
    // 5. [응답 변환] MealPlan -> MealPlanGenerateResponse 리스트 변환
    // =========================================================================
    public List<MealPlanGenerateResponse> toResponseList(MealPlan mealPlan) {
        List<MealPlanMenu> menus = mealPlanMenuRepository.findAllByMealPlanId(mealPlan.getId());

        return menus.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MealPlanGenerateResponse toResponse(MealPlanMenu menu) {
        // 각 메뉴 항목 변환
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

        // 알레르기 요약 생성
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

        // FoodInfo에서 menu_id 조회
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
    // 6. [응답 변환] MealPlan -> MealPlanMonthlyResponse 변환
    // =========================================================================
    public MealPlanMonthlyResponse toMonthlyResponse(MealPlan mealPlan) {
        List<MealPlanMenu> menus = mealPlanMenuRepository.findByMealPlanId(mealPlan.getId());

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

        // FoodInfo에서 food_code 조회
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
                .unique(new ArrayList<>(uniqueAllergens))
                .hasAllergen5(uniqueAllergens.contains(5))
                .build();
    }

    // =========================================================================
    // 7. [상세 조회] 일간 식단표 상세 조회
    // =========================================================================
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

    // =========================================================================
    // 8. [주간 조회] 주간 식단표 조회
    // =========================================================================
    @Transactional(readOnly = true)
    public List<MealPlanMenu> findWeeklyMenus(Long schoolId, LocalDate weekStart, LocalDate weekEnd) {
        return mealPlanMenuRepository.findByMealPlan_SchoolIdAndMenuDateBetweenOrderByMenuDateAscMealTypeAsc(
                schoolId, weekStart, weekEnd
        );
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

        // 각 메뉴 항목 처리
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
    // 9. [AI 대체] 1끼 AI 자동 대체
    // =========================================================================
    @Transactional
    public MealPlanAIReplaceResponse replaceMenuWithAi(Long schoolId, LocalDate date, MealType mealType) {
        log.info("🤖 AI 자동 대체 요청: schoolId={}, date={}, mealType={}", schoolId, date, mealType);

        // 1. 기존 메뉴 조회
        MealPlanMenu menu = mealPlanMenuRepository.findByMealPlan_SchoolIdAndMenuDateAndMealType(
                schoolId, date, mealType
        ).orElseThrow(() -> new IllegalArgumentException("해당 날짜의 식단표를 찾을 수 없습니다."));

        // 2. FastAPI 호출
        String url = String.format("%s/v1/menus/single:generate", fastApiBaseUrl);
        HttpHeaders headers = createHeaders();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("date", date.toString());
        requestBody.put("meal_type", mealType.name());
        requestBody.put("school_id", schoolId);

        // 현재 메뉴 정보도 전달 (AI가 참고할 수 있도록)
        Map<String, String> currentMenus = new HashMap<>();
        currentMenus.put("rice", menu.getRiceDisplay());
        currentMenus.put("soup", menu.getSoupDisplay());
        currentMenus.put("main1", menu.getMain1Display());
        currentMenus.put("main2", menu.getMain2Display());
        currentMenus.put("side", menu.getSideDisplay());
        currentMenus.put("kimchi", menu.getKimchiDisplay());
        currentMenus.put("dessert", menu.getDessertDisplay());
        requestBody.put("current_menus", currentMenus);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 변경 전 메뉴 저장 (히스토리용)
        String oldMenus = buildMenuString(menu);

        log.info("🚀 FastAPI AI 대체 호출: {}", url);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        JsonNode result = Objects.requireNonNull(response.getBody());
        log.info("✅ FastAPI AI 응답 수신");

        // 3. 메뉴 업데이트
        if (result.has("Rice")) menu.setRiceDisplay(result.get("Rice").asText(null));
        if (result.has("Soup")) menu.setSoupDisplay(result.get("Soup").asText(null));
        if (result.has("Main1")) menu.setMain1Display(result.get("Main1").asText(null));
        if (result.has("Main2")) menu.setMain2Display(result.get("Main2").asText(null));
        if (result.has("Side")) menu.setSideDisplay(result.get("Side").asText(null));
        if (result.has("Kimchi")) menu.setKimchiDisplay(result.get("Kimchi").asText(null));
        if (result.has("Dessert")) menu.setDessertDisplay(result.get("Dessert").asText(null));

        // 영양 정보 업데이트
        if (result.has("Kcal")) menu.setKcal(BigDecimal.valueOf(result.get("Kcal").asDouble()));
        if (result.has("Carb")) menu.setCarb(BigDecimal.valueOf(result.get("Carb").asDouble()));
        if (result.has("Prot")) menu.setProt(BigDecimal.valueOf(result.get("Prot").asDouble()));
        if (result.has("Fat")) menu.setFat(BigDecimal.valueOf(result.get("Fat").asDouble()));
        if (result.has("Cost")) menu.setCost(result.get("Cost").asInt());

        // AI 코멘트
        String aiComment = result.has("reason") ? result.get("reason").asText() : "AI 자동 대체";
        menu.setAiComment(aiComment);

        // 4. 저장
        MealPlanMenu savedMenu = mealPlanMenuRepository.save(menu);
        log.info("✅ AI 대체 완료: menuId={}", savedMenu.getId());

        // 변경 후 메뉴 (히스토리용)
        String newMenus = buildMenuString(savedMenu);

        // 5. 히스토리 저장 (알레르기 정보 포함된 실제 메뉴)
        saveHistory(
                date.toString(),
                mealType.name(),
                oldMenus,
                newMenus,
                aiComment,
                MenuHistory.ActionType.AI_AUTO_REPLACE
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
    // 10. [수동 수정] 식단표 수동 수정
    // =========================================================================
    @Transactional
    public MealPlanManualUpdateResponse updateMenuManually(Long mealPlanId, Long menuId, List<String> newMenus, String reason) {
        log.info("✏️ 식단표 수동 수정 요청: mealPlanId={}, menuId={}", mealPlanId, menuId);

        // 1. 메뉴 조회
        MealPlanMenu menu = mealPlanMenuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("해당 식단표를 찾을 수 없습니다."));

        // mealPlanId 검증
        if (!menu.getMealPlan().getId().equals(mealPlanId)) {
            throw new IllegalArgumentException("해당 식단표를 찾을 수 없습니다.");
        }

        String oldMenus = buildMenuString(menu);

        // 2. 메뉴 항목별 매핑 및 영양정보 계산
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

            // FoodInfo에서 조회
            Optional<FoodInfo> foodOpt = foodInfoRepository.findByFoodNameIgnoreSpace(pureName);

            if (foodOpt.isPresent()) {
                FoodInfo food = foodOpt.get();

                // 알레르기 정보 파싱
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

                // 영양 정보 누적
                if (food.getKcal() != null) totalKcal = totalKcal.add(BigDecimal.valueOf(food.getKcal()));
                if (food.getCarbs() != null) totalCarb = totalCarb.add(food.getCarbs());
                if (food.getProtein() != null) totalProt = totalProt.add(food.getProtein());
                if (food.getFat() != null) totalFat = totalFat.add(food.getFat());
            } else {
                displayMenus.add(pureName);
                byMenu.put(pureName, new ArrayList<>());
            }
        }

        // 3. 메뉴 업데이트 (7개 슬롯에 매핑)
        menu.setRiceDisplay(displayMenus.size() > 0 ? displayMenus.get(0) : null);
        menu.setSoupDisplay(displayMenus.size() > 1 ? displayMenus.get(1) : null);
        menu.setMain1Display(displayMenus.size() > 2 ? displayMenus.get(2) : null);
        menu.setMain2Display(displayMenus.size() > 3 ? displayMenus.get(3) : null);
        menu.setSideDisplay(displayMenus.size() > 4 ? displayMenus.get(4) : null);
        menu.setKimchiDisplay(displayMenus.size() > 5 ? displayMenus.get(5) : null);
        menu.setDessertDisplay(displayMenus.size() > 6 ? displayMenus.get(6) : null);

        // 영양 정보 업데이트
        menu.setKcal(totalKcal);
        menu.setCarb(totalCarb);
        menu.setProt(totalProt);
        menu.setFat(totalFat);

        // raw_menus_json 업데이트
        try {
            menu.setRawMenusJson(objectMapper.writeValueAsString(rawMenus));
        } catch (Exception e) {
            log.warn("⚠️ rawMenusJson 변환 실패: {}", e.getMessage());
            menu.setRawMenusJson(rawMenus.toString());
        }

        // 수정 사유 저장
        menu.setAiComment(reason);

        // 4. 저장
        MealPlanMenu savedMenu = mealPlanMenuRepository.save(menu);
        log.info("✅ 수동 수정 완료: menuId={}", savedMenu.getId());

        // 5. 히스토리 저장 (알레르기 정보 포함된 displayMenus 사용)
        saveHistory(
                savedMenu.getMenuDate().toString(),
                savedMenu.getMealType().name(),
                oldMenus,
                String.join(", ", displayMenus),
                reason,
                MenuHistory.ActionType.MANUAL_UPDATE
        );

        // 6. 응답 생성
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

    private String buildMenuString(MealPlanMenu menu) {
        List<String> menus = new ArrayList<>();
        if (menu.getRiceDisplay() != null) menus.add(menu.getRiceDisplay());
        if (menu.getSoupDisplay() != null) menus.add(menu.getSoupDisplay());
        if (menu.getMain1Display() != null) menus.add(menu.getMain1Display());
        if (menu.getMain2Display() != null) menus.add(menu.getMain2Display());
        if (menu.getSideDisplay() != null) menus.add(menu.getSideDisplay());
        if (menu.getKimchiDisplay() != null) menus.add(menu.getKimchiDisplay());
        if (menu.getDessertDisplay() != null) menus.add(menu.getDessertDisplay());
        return String.join(", ", menus);
    }

    // =========================================================================
    // 11. [히스토리 조회] 식단표 수정 히스토리 조회
    // =========================================================================
    @Transactional(readOnly = true)
    public MealPlanHistoryResponse getHistories(String date, String mealType, String actionType, int page, int size) {
        log.info("📜 히스토리 조회: date={}, mealType={}, actionType={}, page={}, size={}",
                date, mealType, actionType, page, size);

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<MenuHistory> historyPage;

        // 조건에 따른 조회
        MenuHistory.ActionType actionTypeEnum = null;
        if (actionType != null && !actionType.isBlank()) {
            try {
                actionTypeEnum = MenuHistory.ActionType.valueOf(actionType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ 유효하지 않은 actionType: {}", actionType);
            }
        }

        boolean hasDate = date != null && !date.isBlank();
        boolean hasMealType = mealType != null && !mealType.isBlank();
        boolean hasActionType = actionTypeEnum != null;

        if (hasDate && hasMealType && hasActionType) {
            historyPage = menuHistoryRepository.findByMealDateAndMealTypeAndActionTypeOrderByIdDesc(
                    date, mealType.toUpperCase(), actionTypeEnum, pageRequest);
        } else if (hasDate && hasMealType) {
            historyPage = menuHistoryRepository.findByMealDateAndMealTypeOrderByIdDesc(
                    date, mealType.toUpperCase(), pageRequest);
        } else if (hasDate && hasActionType) {
            historyPage = menuHistoryRepository.findByMealDateAndActionTypeOrderByIdDesc(
                    date, actionTypeEnum, pageRequest);
        } else if (hasMealType && hasActionType) {
            historyPage = menuHistoryRepository.findByMealTypeAndActionTypeOrderByIdDesc(
                    mealType.toUpperCase(), actionTypeEnum, pageRequest);
        } else if (hasDate) {
            historyPage = menuHistoryRepository.findByMealDateOrderByIdDesc(date, pageRequest);
        } else if (hasMealType) {
            historyPage = menuHistoryRepository.findByMealTypeOrderByIdDesc(mealType.toUpperCase(), pageRequest);
        } else if (hasActionType) {
            historyPage = menuHistoryRepository.findByActionTypeOrderByIdDesc(actionTypeEnum, pageRequest);
        } else {
            historyPage = menuHistoryRepository.findAllByOrderByIdDesc(pageRequest);
        }

        List<MealPlanHistoryResponse.HistoryItem> items = historyPage.getContent().stream()
                .map(this::toHistoryItem)
                .collect(Collectors.toList());

        return MealPlanHistoryResponse.builder()
                .count(items.size())
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
                .build();
    }

    private List<String> parseMenuString(String menuString) {
        if (menuString == null || menuString.isBlank()) {
            return new ArrayList<>();
        }
        // 콤마로 분리하여 리스트로 변환
        return Arrays.stream(menuString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}