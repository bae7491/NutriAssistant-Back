package com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewMenuAnalysisResponse {
    private Boolean success;
    private String message;
    private Object data;
}
