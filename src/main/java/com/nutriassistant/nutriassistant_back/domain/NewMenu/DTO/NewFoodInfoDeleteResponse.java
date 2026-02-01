package com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NewFoodInfoDeleteResponse {

    @JsonProperty("new_food_id")
    private String newFoodId;

    private boolean deleted;

    @JsonProperty("delete_type")
    private String deleteType;

    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
}
