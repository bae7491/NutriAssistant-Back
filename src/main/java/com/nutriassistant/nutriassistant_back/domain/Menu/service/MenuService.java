package com.nutriassistant.nutriassistant_back.domain.Menu.service;

import com.nutriassistant.nutriassistant_back.domain.Menu.DTO.FoodInfoDetailResponse;
import com.nutriassistant.nutriassistant_back.domain.Menu.DTO.FoodInfoListResponse;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.FoodInfo;
import com.nutriassistant.nutriassistant_back.domain.MealPlan.repository.FoodInfoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final FoodInfoRepository foodInfoRepository;

    public MenuService(FoodInfoRepository foodInfoRepository) {
        this.foodInfoRepository = foodInfoRepository;
    }

    @Transactional(readOnly = true)
    public FoodInfoListResponse getFoodInfoList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("foodName").ascending());
        Page<FoodInfo> foodInfoPage = foodInfoRepository.findAll(pageable);

        List<FoodInfoListResponse.FoodInfoItem> items = foodInfoPage.getContent().stream()
                .map(this::toFoodInfoItem)
                .collect(Collectors.toList());

        return FoodInfoListResponse.builder()
                .currentPage(page + 1)
                .pageSize(size)
                .totalPages(foodInfoPage.getTotalPages())
                .totalItems(foodInfoPage.getTotalElements())
                .items(items)
                .build();
    }

    private FoodInfoListResponse.FoodInfoItem toFoodInfoItem(FoodInfo foodInfo) {
        List<Integer> allergens = parseAllergens(foodInfo.getAllergyInfo());

        return FoodInfoListResponse.FoodInfoItem.builder()
                .menuId(foodInfo.getFoodCode())
                .name(foodInfo.getFoodName())
                .category(foodInfo.getCategory())
                .kcal(foodInfo.getKcal())
                .allergens(allergens)
                .updatedAt(foodInfo.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<FoodInfoDetailResponse> getFoodInfoDetail(String menuId) {
        return foodInfoRepository.findByFoodCode(menuId)
                .map(this::toFoodInfoDetail);
    }

    private FoodInfoDetailResponse toFoodInfoDetail(FoodInfo foodInfo) {
        List<Integer> allergens = parseAllergens(foodInfo.getAllergyInfo());

        return FoodInfoDetailResponse.builder()
                .menuId(foodInfo.getFoodCode())
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
                .updatedAt(foodInfo.getUpdatedAt())
                .build();
    }

    private List<Integer> parseAllergens(String allergyInfo) {
        List<Integer> allergens = new ArrayList<>();
        if (allergyInfo == null || allergyInfo.isBlank()) {
            return allergens;
        }

        for (String s : allergyInfo.split(",")) {
            try {
                allergens.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return allergens;
    }
}
