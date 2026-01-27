package com.nutriassistant.nutriassistant_back.MealPlan.service;

import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanCreateRequest;
import com.nutriassistant.nutriassistant_back.MealPlan.DTO.MealPlanGenerateRequest;
import com.nutriassistant.nutriassistant_back.MealPlan.entity.*;
import com.nutriassistant.nutriassistant_back.entity.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
    // [조회]
    // =========================================================================
    @Transactional(readOnly = true)
    public MealPlan getById(Long id) {
        return mealPlanRepository.findByIdWithMenus(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 식단 계획을 찾을 수 없습니다. ID=" + id));
    }

    // =========================================================================
    // 1. [생성] 월간 식단 생성 (FastAPI 호출 -> DB 저장)
    // =========================================================================
    @Transactional
    public MealPlan generateAndSave(MealPlanGenerateRequest req) {
        log.info("============================================================");
        log.info("📋 식단 생성 요청 시작");
        log.info("============================================================");
        log.info("   연도/월: {}/{}", req.getYear(), req.getMonth());

        // ========================================
        // 1. DB에서 이전 달 리포트 조회
        // ========================================
        JsonNode reportData = null;

        // 이전 달 계산
        int reportYear = req.getYear();
        int reportMonth = req.getMonth() - 1;
        if (reportMonth == 0) {
            reportMonth = 12;
            reportYear -= 1;
        }

        log.info("📊 리포트 조회 시도: {}년 {}월", reportYear, reportMonth);

        Optional<Report> reportOpt = reportService.findByYearAndMonth(reportYear, reportMonth);

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
        requestBody.put("year", req.getYear());
        requestBody.put("month", req.getMonth());

        // Options 추가
        if (req.getOptions() != null) {
            requestBody.put("options", req.getOptions());
        }

        // ✅ DB에서 조회한 리포트 추가
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
                req.getYear(),
                req.getMonth(),
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
        log.info("💾 DB 저장: {}년 {}월", req.year(), req.month());

        MealPlan mealPlan = mealPlanRepository.findByYearAndMonth(req.year(), req.month())
                .orElseGet(() -> {
                    log.info("   신규 MealPlan 생성");
                    return new MealPlan(req.year(), req.month(), req.generatedAt());
                });

        if (mealPlan.getId() != null) {
            log.info("   기존 MealPlan 갱신: ID={}", mealPlan.getId());
        }

        mealPlan.setGeneratedAt(req.generatedAt());
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
    // 2. [AI 수정] 원클릭 메뉴 대체 (1끼)
    // =========================================================================
    @Transactional
    public void replaceMenuWithAi(String dateStr, String mealTypeStr) {
        LocalDate date = LocalDate.parse(dateStr);
        MealType mealType = MealType.valueOf(mealTypeStr);

        // FastAPI 요청
        String url = String.format("%s/v1/menus/single:generate", fastApiBaseUrl);
        HttpHeaders headers = createHeaders();
        Map<String, String> body = Map.of("date", dateStr, "meal_type", mealTypeStr);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> result = response.getBody();

        // 8개 후보군 검증
        System.out.println("\n🕵️ [AI 로직 검증] 8개 후보군 생성 여부 확인");
        if (result != null) {
            if (result.containsKey("candidates")) {
                List<?> candidates = (List<?>) result.get("candidates");
                int count = candidates.size();
                System.out.println("✅ 'candidates' 데이터 발견됨!");
                System.out.println("📊 생성된 후보 개수: " + count + "개");
                if (count == 8) {
                    System.out.println("🎉 검증 성공: 8개의 후보 중에서 최적의 식단이 선택되었습니다.");
                } else {
                    System.out.println("⚠️ 검증 경고: 후보 개수가 8개가 아닙니다 (" + count + "개).");
                }
            } else {
                System.out.println("⚠️ 'candidates' 키가 없습니다.");
            }
        }
        System.out.println("--------------------------------------------------\n");

        // Python 응답 디버깅
        System.out.println("=== Python AI 응답 ===");
        System.out.println(result);
        System.out.println("menus: " + result.get("menus"));
        System.out.println("rawMenus: " + result.get("rawMenus"));
        System.out.println("dessert: " + result.get("dessert"));
        System.out.println("kcal: " + result.get("kcal"));
        System.out.println("carb: " + result.get("carb"));
        System.out.println("prot: " + result.get("prot"));
        System.out.println("fat: " + result.get("fat"));
        System.out.println("cost: " + result.get("cost"));
        System.out.println("====================");

        MealPlanMenu menu = mealPlanMenuRepository.findByDateAndType(date, mealType)
                .orElseThrow(() -> new IllegalArgumentException("수정할 식단 데이터가 없습니다."));

        String oldMenus = menu.getMenuString();

        List<String> newMenus = (List<String>) result.get("menus");
        List<String> rawMenus = (List<String>) result.get("rawMenus");
        String aiReason = (String) result.get("reason");

        // ★★★ 모든 정보를 한 번에 업데이트 ★★★
        // 1. 메뉴 정보 업데이트
        menu.updateMenus(newMenus);
        menu.updateRawMenus(rawMenus, objectMapper);
        menu.setAiComment(aiReason);

        // 2. 영양 정보 업데이트
        if (result.get("kcal") != null) {
            Double kcalValue = Double.valueOf(result.get("kcal").toString());
            System.out.println("🔄 kcal 업데이트: " + menu.getKcal() + " -> " + kcalValue);
            menu.setKcal(kcalValue);
        }
        if (result.get("carb") != null) {
            Double carbValue = Double.valueOf(result.get("carb").toString());
            System.out.println("🔄 carb 업데이트: " + menu.getCarb() + " -> " + carbValue);
            menu.setCarb(carbValue);
        }
        if (result.get("prot") != null) {
            Double protValue = Double.valueOf(result.get("prot").toString());
            System.out.println("🔄 prot 업데이트: " + menu.getProt() + " -> " + protValue);
            menu.setProt(protValue);
        }
        if (result.get("fat") != null) {
            Double fatValue = Double.valueOf(result.get("fat").toString());
            System.out.println("🔄 fat 업데이트: " + menu.getFat() + " -> " + fatValue);
            menu.setFat(fatValue);
        }

        // 3. 비용 정보 업데이트
        if (result.get("cost") != null) {
            Integer costValue = Integer.valueOf(result.get("cost").toString());
            System.out.println("🔄 cost 업데이트: " + menu.getCost() + " -> " + costValue);
            menu.setCost(costValue);
        }

        // 4. DB 저장 (한 번만!)
        mealPlanMenuRepository.save(menu);

        // 5. 히스토리 저장 (한 번만!)
        saveHistory(dateStr, mealTypeStr, oldMenus, newMenus.toString(), aiReason, MenuHistory.ActionType.AI_AUTO_REPLACE);

        System.out.println("✅ 업데이트 완료!");
    }

    // =========================================================================
    // 3. [수동 수정] 사용자가 직접 메뉴 입력
    // =========================================================================
    @Transactional
    public void updateMenuManually(String dateStr, String mealTypeStr, List<String> newMenus, String reason) {
        // 1. 날짜 및 타입 파싱
        LocalDate date = LocalDate.parse(dateStr);
        MealType mealType = MealType.valueOf(mealTypeStr);

        // 2. 기존 식단 데이터 조회
        MealPlanMenu menu = mealPlanMenuRepository.findByDateAndType(date, mealType)
                .orElseThrow(() -> new IllegalArgumentException("수정할 식단 데이터가 없습니다."));

        String oldMenus = menu.getMenuString();

        // 3. 변수 초기화
        List<String> finalDisplayMenus = new ArrayList<>();
        List<String> pureRawMenus = new ArrayList<>();

        // [수정] 영양소 합산용 변수 (계산은 double로 하고 나중에 Entity에 Double로 넣음)
        int totalKcal = 0;
        double totalCarb = 0;
        double totalProt = 0;
        double totalFat = 0;

        // --- [로직 시작] 입력된 메뉴 리스트 순회 ---
        for (String inputMenuName : newMenus) {
            String pureName = inputMenuName.replaceAll("\\s*\\([^)]*\\)", "").trim();
            if (pureName.isEmpty()) continue;
            pureRawMenus.add(pureName);

            // (2) 1차 시도: Repository Query로 검색
            Optional<FoodInfo> foodOpt = foodInfoRepository.findByFoodNameIgnoreSpace(pureName);

            // (3) [비상 대책] 2차 시도: 전수 조사 (DB 쿼리가 실패할 경우 대비)
            if (foodOpt.isEmpty()) {
                System.out.println("⚠️ Query 검색 실패: [" + pureName + "] -> 전수 조사 시도");
                List<FoodInfo> allFoods = foodInfoRepository.findAll();

                for (FoodInfo dbFood : allFoods) {
                    String dbNameClean = dbFood.getFoodName().replace(" ", "");
                    String inputNameClean = pureName.replace(" ", "");

                    if (dbNameClean.equals(inputNameClean)) {
                        System.out.println("✅ [전수 조사 성공] (" + dbFood.getFoodName() + ")");
                        foodOpt = Optional.of(dbFood);
                        break;
                    }
                }
            }

            // (4) 데이터 처리
            if (foodOpt.isPresent()) {
                FoodInfo food = foodOpt.get();

                // 4-1. 알레르기 정보
                String allergy = (food.getAllergyInfo() != null && !food.getAllergyInfo().isEmpty())
                        ? "(" + food.getAllergyInfo() + ")" : "";
                finalDisplayMenus.add(pureName + allergy);

                // 4-2. 영양 성분 누적 ([수정] BigDecimal -> double 변환 후 누적)
                totalKcal += (food.getKcal() != null) ? food.getKcal() : 0;
                totalCarb += (food.getCarbs() != null) ? food.getCarbs().doubleValue() : 0;
                totalProt += (food.getProtein() != null) ? food.getProtein().doubleValue() : 0;
                totalFat += (food.getFat() != null) ? food.getFat().doubleValue() : 0;

                System.out.println("🆗 매핑 완료: " + pureName);
            } else {
                finalDisplayMenus.add(pureName);
                System.out.println("❌ 실패: DB에 없음 -> [" + pureName + "]");
            }
        }

        // --- [저장 단계] ---
        try {
            menu.updateMenus(finalDisplayMenus);
            String rawJson = objectMapper.writeValueAsString(pureRawMenus);
            menu.setRawMenusJson(rawJson);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 오류", e);
        }

        // [수정] 합산된 영양 정보 저장
        // Entity(MealPlanMenu)의 필드는 Double 타입입니다.
        // 따라서 계산된 double 값들을 그대로 넣어주어야 합니다. (int 강제 변환 금지)
        menu.setKcal((double) totalKcal);
        menu.setCarb(totalCarb);
        menu.setProt(totalProt);
        menu.setFat(totalFat);

        // 기타 정보 저장
        menu.setAiComment(reason);
        mealPlanMenuRepository.save(menu);

        saveHistory(dateStr, mealTypeStr, oldMenus, finalDisplayMenus.toString(), reason, MenuHistory.ActionType.MANUAL_UPDATE);
    }

//    // =========================================================================
//    // 4. [헬퍼] 공통 내부 메서드
//    // =========================================================================
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

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (internalToken != null && !internalToken.isEmpty()) {
            headers.set("X-Internal-Token", internalToken);
        }
        return headers;
    }
}