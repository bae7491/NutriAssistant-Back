package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class FastApiDto {

    // Spring Boot -> FastAPI (보낼 때)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long schoolId;
        private String targetDate;
        private List<String> reviewTexts;
    }

    // FastAPI -> Spring Boot (받을 때)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    // @Builder // 받는 용도라 빌더는 필수는 아니지만 있어도 무방함
    public static class Response {
        private String sentimentLabel;
        private Float sentimentScore;
        private Float sentimentConf;

        // [이 부분들이 빠져 있어서 에러가 난 것입니다]
        private Integer positiveCount; // 긍정 개수
        private Integer negativeCount; // 부정 개수

        private List<String> aspectTags;
        private List<String> evidencePhrases;
        private Boolean issueFlags;
    }
}