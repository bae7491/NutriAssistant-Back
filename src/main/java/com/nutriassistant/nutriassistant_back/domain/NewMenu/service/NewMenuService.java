package com.nutriassistant.nutriassistant_back.domain.NewMenu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.Board;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewFoodInfoCreateRequest;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewFoodInfoDeleteResponse;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewFoodInfoResponse;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewFoodInfoUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewMenuAnalysisResponse;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.entity.NewFoodInfo;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.repository.NewFoodInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NewMenuService {

    private final RestTemplate restTemplate;
    private final NewFoodInfoRepository newFoodInfoRepository;

    @Value("${fastapi.base-url:http://localhost:8001}")
    private String fastApiBaseUrl;

    @Value("${fastapi.internal-token:}")
    private String internalToken;

    public NewMenuService(RestTemplate restTemplate, NewFoodInfoRepository newFoodInfoRepository) {
        this.restTemplate = restTemplate;
        this.newFoodInfoRepository = newFoodInfoRepository;
    }

    /**
     * 신메뉴 분석 비동기 요청 (게시글 등록 후 백그라운드 실행)
     */
    @Async
    public void requestAnalysisAsync(Board board) {
        log.info("🔄 비동기 신메뉴 분석 시작: boardId={}", board.getId());
        requestAnalysis(board);
    }

    /**
     * 신메뉴 분석 요청 (FastAPI 호출)
     */
    public NewMenuAnalysisResponse requestAnalysis(Board board) {
        log.info("🤖 신메뉴 분석 요청 시작: boardId={}", board.getId());

        try {
            String url = String.format("%s/v1/menus/new-menu:generate", fastApiBaseUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (internalToken != null && !internalToken.isBlank()) {
                headers.set("X-Internal-API-Key", internalToken);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("board_id", board.getId());
            requestBody.put("school_id", board.getSchoolId());
            requestBody.put("title", board.getTitle());
            requestBody.put("content", board.getContent());
            requestBody.put("author_type", board.getAuthorType().name());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("🚀 FastAPI 신메뉴 분석 호출: {}", url);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            JsonNode result = response.getBody();
            log.info("✅ FastAPI 신메뉴 분석 응답 수신");
            log.info("📄 응답 내용: {}", result != null ? result.toString() : "null");

            // 분석 결과를 DB에 저장
            if (result != null) {
                saveNewFoodInfo(result);
            }

            return NewMenuAnalysisResponse.builder()
                    .success(true)
                    .message("신메뉴 분석 완료")
                    .data(result)
                    .build();

        } catch (Exception e) {
            log.error("❌ FastAPI 신메뉴 분석 실패: {}", e.getMessage());

            return NewMenuAnalysisResponse.builder()
                    .success(false)
                    .message("신메뉴 분석 요청 실패: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }

    /**
     * FastAPI 분석 결과를 NewFoodInfo 테이블에 저장
     * 응답 형식: { "generated_at": "...", "new_menus": [...], "analysis_summary": {...} }
     */
    private void saveNewFoodInfo(JsonNode result) {
        try {
            // new_menus 배열 추출
            JsonNode newMenus = result.get("new_menus");
            if (newMenus != null && !newMenus.isNull() && newMenus.isArray()) {
                log.info("📦 응답 형식: new_menus 배열 ({}개)", newMenus.size());
                int savedCount = 0;
                for (JsonNode item : newMenus) {
                    if (saveNewFoodInfoItem(item)) {
                        savedCount++;
                    }
                }
                log.info("✅ 총 {}개 신메뉴 저장 완료", savedCount);
                return;
            }

            log.warn("⚠️ new_menus 배열을 찾을 수 없음");

        } catch (Exception e) {
            log.error("❌ 신메뉴 정보 저장 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 개별 메뉴 항목을 DB에 저장
     * @return 저장 성공 여부
     */
    private boolean saveNewFoodInfoItem(JsonNode data) {
        String menuName = getTextValue(data, "menu_name");

        if (menuName == null || menuName.isBlank()) {
            log.warn("⚠️ menu_name이 없어서 저장 건너뜀");
            return false;
        }

        // 동일한 메뉴명이 이미 존재하면 저장 건너뜀
        if (newFoodInfoRepository.existsByFoodName(menuName)) {
            log.info("ℹ️ 이미 존재하는 메뉴: {}", menuName);
            return false;
        }

        // NEWFOOD-N 형식으로 food_code 자동 생성
        String foodCode = generateNextFoodCode();

        // nutrition 객체에서 영양 정보 추출
        JsonNode nutrition = data.get("nutrition");

        NewFoodInfo newFoodInfo = new NewFoodInfo();
        newFoodInfo.setFoodCode(foodCode);
        newFoodInfo.setFoodName(menuName);
        newFoodInfo.setCategory(getTextValue(data, "category"));

        // 영양 정보 (nutrition 객체 내부)
        if (nutrition != null && !nutrition.isNull()) {
            newFoodInfo.setKcal(getIntegerValue(nutrition, "kcal"));
            newFoodInfo.setCarbs(getBigDecimalValue(nutrition, "carbs"));
            newFoodInfo.setProtein(getBigDecimalValue(nutrition, "protein"));
            newFoodInfo.setFat(getBigDecimalValue(nutrition, "fat"));
            newFoodInfo.setCalcium(getBigDecimalValue(nutrition, "calcium"));
            newFoodInfo.setIron(getBigDecimalValue(nutrition, "iron"));
            newFoodInfo.setVitaminA(getBigDecimalValue(nutrition, "vitamin_a"));
            newFoodInfo.setThiamin(getBigDecimalValue(nutrition, "thiamin"));
            newFoodInfo.setRiboflavin(getBigDecimalValue(nutrition, "riboflavin"));
            newFoodInfo.setVitaminC(getBigDecimalValue(nutrition, "vitamin_c"));
            newFoodInfo.setServingBasis(getTextValue(nutrition, "serving_basis"));
            // food_weight는 숫자 또는 문자열일 수 있음
            JsonNode foodWeightNode = nutrition.get("food_weight");
            if (foodWeightNode != null && !foodWeightNode.isNull()) {
                newFoodInfo.setFoodWeight(foodWeightNode.asText());
            }
        }

        // 재료 (배열 → 문자열) - DB 컬럼 크기에 맞게 truncate
        newFoodInfo.setIngredients(truncateIfNeeded(parseArrayToString(data.get("ingredients"), ", "), 255));

        // 레시피 (배열 → 문자열) - DB 컬럼 크기에 맞게 truncate
        newFoodInfo.setRecipe(truncateIfNeeded(parseArrayToString(data.get("recipe_steps"), "\n"), 255));

        // 알레르기 정보 (숫자 배열 → 문자열)
        newFoodInfo.setAllergyInfo(parseAllergens(data.get("allergens")));

        newFoodInfoRepository.save(newFoodInfo);
        log.info("✅ 신메뉴 저장: {} ({})", menuName, getTextValue(data, "category"));
        return true;
    }

    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    private Integer getIntegerValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            return field.asInt();
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null && !field.isNull()) {
            try {
                return new BigDecimal(field.asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 문자열 길이 제한 (DB 컬럼 크기 초과 방지)
     */
    private String truncateIfNeeded(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        log.warn("⚠️ 텍스트 길이 초과로 잘림: {} → {}", text.length(), maxLength);
        return text.substring(0, maxLength);
    }

    /**
     * 배열을 문자열로 변환
     */
    private String parseArrayToString(JsonNode arrayNode, String delimiter) {
        if (arrayNode == null || arrayNode.isNull() || !arrayNode.isArray()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayNode.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(arrayNode.get(i).asText());
        }
        return sb.toString();
    }

    /**
     * allergens 배열을 문자열로 변환 (예: [1, 5, 6] → "1, 5, 6")
     */
    private String parseAllergens(JsonNode allergensNode) {
        return parseArrayToString(allergensNode, ", ");
    }

    /**
     * 다음 NEWFOOD-N 코드 생성
     */
    private String generateNextFoodCode() {
        Integer maxNumber = newFoodInfoRepository.findMaxFoodCodeNumber();
        int nextNumber = (maxNumber == null) ? 1 : maxNumber + 1;
        return "NEWFOOD-" + nextNumber;
    }

    /**
     * 신메뉴 직접 등록
     */
    public NewFoodInfoResponse createNewFoodInfo(NewFoodInfoCreateRequest request) {
        // 중복 체크
        if (newFoodInfoRepository.existsByFoodName(request.getName())) {
            throw new IllegalStateException("DUPLICATE:" + request.getName());
        }

        String foodCode = generateNextFoodCode();

        NewFoodInfo newFoodInfo = new NewFoodInfo();
        newFoodInfo.setFoodCode(foodCode);
        newFoodInfo.setFoodName(request.getName());
        newFoodInfo.setCategory(request.getCategory());
        newFoodInfo.setServingBasis(request.getNutritionBasis());
        newFoodInfo.setFoodWeight(request.getServingSize());
        newFoodInfo.setKcal(request.getKcal());
        newFoodInfo.setCarbs(request.getCarb());
        newFoodInfo.setProtein(request.getProt());
        newFoodInfo.setFat(request.getFat());
        newFoodInfo.setCalcium(request.getCalcium());
        newFoodInfo.setIron(request.getIron());
        newFoodInfo.setVitaminA(request.getVitaminA());
        newFoodInfo.setThiamin(request.getThiamin());
        newFoodInfo.setRiboflavin(request.getRiboflavin());
        newFoodInfo.setVitaminC(request.getVitaminC());
        newFoodInfo.setIngredients(request.getIngredientsText());
        newFoodInfo.setRecipe(request.getRecipeText());

        // 알레르기 정보 변환 (List<Integer> -> String)
        if (request.getAllergens() != null && !request.getAllergens().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < request.getAllergens().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(request.getAllergens().get(i));
            }
            newFoodInfo.setAllergyInfo(sb.toString());
        }

        NewFoodInfo saved = newFoodInfoRepository.save(newFoodInfo);
        log.info("✅ 신메뉴 등록 완료: {} ({})", saved.getFoodName(), saved.getFoodCode());

        return toNewFoodInfoResponse(saved);
    }

    /**
     * 신메뉴 삭제 (Soft Delete)
     */
    public NewFoodInfoDeleteResponse deleteNewFoodInfo(String newFoodId) {
        NewFoodInfo foodInfo = newFoodInfoRepository.findByFoodCode(newFoodId)
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND:" + newFoodId));

        // 이미 삭제된 경우
        if (Boolean.TRUE.equals(foodInfo.getDeleted())) {
            throw new IllegalArgumentException("NOT_FOUND:" + newFoodId);
        }

        LocalDateTime now = LocalDateTime.now();
        foodInfo.setDeleted(true);
        foodInfo.setDeletedAt(now);
        newFoodInfoRepository.save(foodInfo);

        log.info("✅ 신메뉴 삭제 완료: {} ({})", foodInfo.getFoodName(), foodInfo.getFoodCode());

        return NewFoodInfoDeleteResponse.builder()
                .newFoodId(newFoodId)
                .deleted(true)
                .deleteType("SOFT")
                .deletedAt(now)
                .build();
    }

    /**
     * 신메뉴 수정
     */
    public NewFoodInfoResponse updateNewFoodInfo(String newMenuId, NewFoodInfoUpdateRequest request) {
        // 기존 메뉴 조회
        NewFoodInfo foodInfo = newFoodInfoRepository.findByFoodCode(newMenuId)
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND:" + newMenuId));

        // 이름 변경 시 중복 체크
        if (request.getName() != null && !request.getName().equals(foodInfo.getFoodName())) {
            if (newFoodInfoRepository.existsByFoodName(request.getName())) {
                throw new IllegalStateException("DUPLICATE:" + request.getName());
            }
            foodInfo.setFoodName(request.getName());
        }

        // 각 필드 업데이트 (null이 아닌 경우에만)
        if (request.getCategory() != null) foodInfo.setCategory(request.getCategory());
        if (request.getNutritionBasis() != null) foodInfo.setServingBasis(request.getNutritionBasis());
        if (request.getServingSize() != null) foodInfo.setFoodWeight(request.getServingSize());
        if (request.getKcal() != null) foodInfo.setKcal(request.getKcal());
        if (request.getCarb() != null) foodInfo.setCarbs(request.getCarb());
        if (request.getProt() != null) foodInfo.setProtein(request.getProt());
        if (request.getFat() != null) foodInfo.setFat(request.getFat());
        if (request.getCalcium() != null) foodInfo.setCalcium(request.getCalcium());
        if (request.getIron() != null) foodInfo.setIron(request.getIron());
        if (request.getVitaminA() != null) foodInfo.setVitaminA(request.getVitaminA());
        if (request.getThiamin() != null) foodInfo.setThiamin(request.getThiamin());
        if (request.getRiboflavin() != null) foodInfo.setRiboflavin(request.getRiboflavin());
        if (request.getVitaminC() != null) foodInfo.setVitaminC(request.getVitaminC());
        if (request.getIngredientsText() != null) foodInfo.setIngredients(request.getIngredientsText());
        if (request.getRecipeText() != null) foodInfo.setRecipe(request.getRecipeText());

        // 알레르기 정보 변환
        if (request.getAllergens() != null) {
            if (request.getAllergens().isEmpty()) {
                foodInfo.setAllergyInfo(null);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < request.getAllergens().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(request.getAllergens().get(i));
                }
                foodInfo.setAllergyInfo(sb.toString());
            }
        }

        NewFoodInfo saved = newFoodInfoRepository.save(foodInfo);
        log.info("✅ 신메뉴 수정 완료: {} ({})", saved.getFoodName(), saved.getFoodCode());

        return toNewFoodInfoResponse(saved);
    }

    /**
     * 신메뉴 목록 조회 (페이지네이션)
     */
    public Page<NewFoodInfoResponse> getNewFoodInfoList(Pageable pageable) {
        Page<NewFoodInfo> page = newFoodInfoRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable);
        return page.map(this::toNewFoodInfoResponse);
    }

    /**
     * 신메뉴 상세 조회
     */
    public NewFoodInfoResponse getNewFoodInfo(String newFoodId) {
        NewFoodInfo foodInfo = newFoodInfoRepository.findByFoodCode(newFoodId)
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND:" + newFoodId));

        if (Boolean.TRUE.equals(foodInfo.getDeleted())) {
            throw new IllegalArgumentException("NOT_FOUND:" + newFoodId);
        }

        return toNewFoodInfoResponse(foodInfo);
    }

    private NewFoodInfoResponse toNewFoodInfoResponse(NewFoodInfo foodInfo) {
        List<Integer> allergens = new ArrayList<>();
        if (foodInfo.getAllergyInfo() != null && !foodInfo.getAllergyInfo().isBlank()) {
            for (String s : foodInfo.getAllergyInfo().split(",")) {
                try {
                    allergens.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return NewFoodInfoResponse.builder()
                .newMenuId(foodInfo.getFoodCode())
                .name(foodInfo.getFoodName())
                .category(foodInfo.getCategory())
                .nutritionBasis(foodInfo.getServingBasis())
                .servingSize(foodInfo.getFoodWeight())
                .kcal(foodInfo.getKcal())
                .carb(foodInfo.getCarbs())
                .prot(foodInfo.getProtein())
                .fat(foodInfo.getFat())
                .calcium(foodInfo.getCalcium())
                .iron(foodInfo.getIron())
                .vitaminA(foodInfo.getVitaminA())
                .thiamin(foodInfo.getThiamin())
                .riboflavin(foodInfo.getRiboflavin())
                .vitaminC(foodInfo.getVitaminC())
                .ingredientsText(foodInfo.getIngredients())
                .allergens(allergens)
                .recipeText(foodInfo.getRecipe())
                .createdAt(foodInfo.getCreatedAt())
                .updatedAt(foodInfo.getUpdatedAt())
                .build();
    }
}
