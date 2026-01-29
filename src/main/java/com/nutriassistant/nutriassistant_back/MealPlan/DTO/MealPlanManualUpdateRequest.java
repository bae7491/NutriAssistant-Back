package com.nutriassistant.nutriassistant_back.MealPlan.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MealPlanManualUpdateRequest {

    @NotBlank(message = "수정 사유는 필수입니다.")
    private String reason;

    @NotEmpty(message = "메뉴 목록은 필수입니다.")
    private List<String> menus;
}
