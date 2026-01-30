package com.nutriassistant.nutriassistant_back.domain.MealPlan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.MenuCostDatabaseResponse;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.MenuCostResponse;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.DTO.MenuCostUploadRequest;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MenuCost;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.MenuCostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuCostService {

    // ✅ Logger 선언
    private static final Logger logger = LoggerFactory.getLogger(MenuCostService.class);

    private final MenuCostRepository menuCostRepository;
    private final ObjectMapper objectMapper;

    @Value("${cost.base-year:2023}")
    private int baseYear;

    @Value("${cost.default-price:1000}")
    private int defaultPrice;

    public MenuCostService(
            MenuCostRepository menuCostRepository,
            ObjectMapper objectMapper
    ) {
        this.menuCostRepository = menuCostRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 서버 시작 시 자동 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCostDB() {
        logger.info("=".repeat(60));
        logger.info("💰 [단가 DB] 초기화 시작");
        logger.info("=".repeat(60));

        long count = menuCostRepository.count();

        if (count > 0) {
            logger.info("✅ 기존 단가 DB 발견: {}개 메뉴", count);

            List<MenuCost> sample = menuCostRepository.findAll();
            if (!sample.isEmpty()) {
                Integer currentYear = sample.get(0).getCurrentYear();
                logger.info("   - 현재 기준 연도: {}년", currentYear);
            }
        } else {
            logger.info("ℹ️ 단가 DB가 비어있습니다 (정상 동작)");
            logger.info("   - 기본 단가 {}원으로 식단 생성 가능", defaultPrice);
            logger.info("   - FastAPI에서 첫 식단 생성 시 AI가 자동으로 단가 생성");
            logger.info("   - 또는 JSON 파일 직접 업로드 가능:");
            logger.info("     POST /api/costs/upload");
        }

        logger.info("=".repeat(60));
    }

    /**
     * JSON 파일 업로드 및 DB 저장
     */
    @Transactional
    public MenuCostDatabaseResponse uploadFromJson(MultipartFile file) throws IOException {
        logger.info("📤 단가 DB 업로드 시작: {}", file.getOriginalFilename());

        // 1. JSON 파싱
        JsonNode root = objectMapper.readTree(file.getInputStream());

        JsonNode metaNode = root.path("meta");
        Integer year = metaNode.path("year").asInt(0);

        if (year == 0) {
            throw new IllegalArgumentException("JSON에 meta.year 정보가 없습니다");
        }

        JsonNode pricesNode = root.path("prices");
        if (!pricesNode.isObject()) {
            throw new IllegalArgumentException("JSON에 prices 객체가 없습니다");
        }

        // 2. 물가상승률 계산
        double inflationMultiplier = calculateInflationMultiplier(baseYear, year);

        // 3. 기존 데이터 삭제 (연도가 다르면)
        List<MenuCost> existing = menuCostRepository.findAll();
        if (!existing.isEmpty() && !existing.get(0).getCurrentYear().equals(year)) {
            logger.info("🗑️ 기존 단가 DB 삭제 (연도 변경: {} → {})",
                    existing.get(0).getCurrentYear(), year);
            menuCostRepository.deleteAll();
        }

        // 4. DB 저장
        int savedCount = 0;
        Map<String, Integer> pricesMap = new HashMap<>();
        List<MenuCost> batchList = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> fields = pricesNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String menuName = entry.getKey().trim();
            Integer price = entry.getValue().asInt();

            pricesMap.put(menuName, price);

            MenuCost menuCost = new MenuCost();
            menuCost.setMenuName(menuName);
            menuCost.setPrice(price);
            menuCost.setBaseYear(baseYear);
            menuCost.setCurrentYear(year);
            menuCost.setInflationMultiplier(inflationMultiplier);

            batchList.add(menuCost);
            savedCount++;

            // 배치 저장 (100개씩)
            if (batchList.size() >= 100) {
                menuCostRepository.saveAll(batchList);
                batchList.clear();
            }
        }

        // 남은 데이터 저장
        if (!batchList.isEmpty()) {
            menuCostRepository.saveAll(batchList);
        }

        logger.info("✅ 단가 DB 업로드 완료: {}개", savedCount);

        return new MenuCostDatabaseResponse(year, savedCount, pricesMap);
    }

    /**
     * 단가 정보 조회 (없으면 기본값 반환)
     */
    @Transactional(readOnly = true)
    public MenuCostResponse getCost(String menuName) {
        Optional<MenuCost> costOpt = menuCostRepository.findByMenuName(menuName.trim());

        if (costOpt.isPresent()) {
            MenuCost cost = costOpt.get();
            return new MenuCostResponse(
                    cost.getMenuName(),
                    cost.getPrice(),
                    cost.getBaseYear(),
                    cost.getCurrentYear(),
                    cost.getInflationMultiplier()
            );
        } else {
            // 단가 정보가 없으면 기본값 반환
            int currentYear = java.time.Year.now().getValue();
            return new MenuCostResponse(
                    menuName,
                    defaultPrice,
                    baseYear,
                    currentYear,
                    1.0
            );
        }
    }

    /**
     * 전체 단가 DB 조회 (FastAPI용)
     */
    @Transactional(readOnly = true)
    public MenuCostDatabaseResponse getAllCosts() {
        List<MenuCost> allCosts = menuCostRepository.findAll();

        if (allCosts.isEmpty()) {
            int currentYear = java.time.Year.now().getValue();
            return new MenuCostDatabaseResponse(
                    currentYear,
                    0,
                    Collections.emptyMap()
            );
        }

        Integer year = allCosts.get(0).getCurrentYear();
        Map<String, Integer> pricesMap = allCosts.stream()
                .collect(Collectors.toMap(
                        MenuCost::getMenuName,
                        MenuCost::getPrice,
                        (existing, replacement) -> existing
                ));

        return new MenuCostDatabaseResponse(year, pricesMap.size(), pricesMap);
    }

    /**
     * DB 상태 확인
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStatus() {
        long count = menuCostRepository.count();

        Map<String, Object> status = new HashMap<>();
        status.put("loaded", count > 0);
        status.put("totalMenus", count);
        status.put("defaultPrice", defaultPrice);

        if (count > 0) {
            List<MenuCost> sample = menuCostRepository.findAll();
            MenuCost first = sample.get(0);

            status.put("currentYear", first.getCurrentYear());
            status.put("baseYear", first.getBaseYear());
            status.put("inflationMultiplier", first.getInflationMultiplier());
            status.put("message", "단가 DB 사용 중");

            int minPrice = sample.stream()
                    .mapToInt(MenuCost::getPrice)
                    .min()
                    .orElse(0);
            int maxPrice = sample.stream()
                    .mapToInt(MenuCost::getPrice)
                    .max()
                    .orElse(0);

            status.put("priceRange", Map.of(
                    "min", minPrice,
                    "max", maxPrice
            ));
        } else {
            int currentYear = java.time.Year.now().getValue();
            status.put("currentYear", currentYear);
            status.put("baseYear", baseYear);
            status.put("inflationMultiplier", 1.0);
            status.put("message", "단가 DB 비어있음 (FastAPI에서 자동 생성 예정)");
        }

        return status;
    }

    /**
     * 단가 일괄 업데이트 (FastAPI AI 생성용)
     */
    @Transactional
    public int bulkUpdate(MenuCostUploadRequest request) {
        logger.info("💾 단가 일괄 업데이트 시작: {}개", request.prices().size());

        double inflationMultiplier = calculateInflationMultiplier(
                baseYear,
                request.year()
        );

        int updateCount = 0;
        List<MenuCost> batchList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : request.prices().entrySet()) {
            String menuName = entry.getKey().trim();
            Integer price = entry.getValue();

            MenuCost cost = new MenuCost();
            cost.setMenuName(menuName);
            cost.setPrice(price);
            cost.setBaseYear(baseYear);
            cost.setCurrentYear(request.year());
            cost.setInflationMultiplier(inflationMultiplier);

            batchList.add(cost);
            updateCount++;

            // 배치 저장 (100개씩)
            if (batchList.size() >= 100) {
                menuCostRepository.saveAll(batchList);
                logger.info("   진행 중: {}/{}개 저장", updateCount, request.prices().size());
                batchList.clear();
            }
        }

        // 남은 데이터 저장
        if (!batchList.isEmpty()) {
            menuCostRepository.saveAll(batchList);
        }

        logger.info("✅ 일괄 업데이트 완료: {}개", updateCount);
        return updateCount;
    }

    /**
     * 물가상승률 계산 (복리)
     */
    private double calculateInflationMultiplier(int fromYear, int toYear) {
        Map<Integer, Double> inflationRates = Map.of(
                2023, 0.036,
                2024, 0.023,
                2025, 0.021
        );
        double annualRate = 0.022;

        double multiplier = 1.0;
        for (int year = fromYear; year < toYear; year++) {
            double rate = inflationRates.getOrDefault(year, annualRate);
            multiplier *= (1 + rate);
        }

        return multiplier;
    }

    /**
     * 연도별 재계산 (물가상승 반영)
     */
    @Transactional
    public int recalculateForNewYear(int newYear) {
        List<MenuCost> allCosts = menuCostRepository.findAll();

        if (allCosts.isEmpty()) {
            throw new IllegalStateException("단가 DB가 비어있습니다");
        }

        double newMultiplier = calculateInflationMultiplier(baseYear, newYear);

        for (MenuCost cost : allCosts) {
            // 기준 연도 가격으로 환원 후 새 연도로 재계산
            double basePrice = cost.getPrice() / cost.getInflationMultiplier();
            int newPrice = (int) Math.round(basePrice * newMultiplier);

            cost.setPrice(newPrice);
            cost.setCurrentYear(newYear);
            cost.setInflationMultiplier(newMultiplier);
        }

        menuCostRepository.saveAll(allCosts);

        logger.info("✅ {}년 단가 재계산 완료: {}개", newYear, allCosts.size());

        return allCosts.size();
    }
}