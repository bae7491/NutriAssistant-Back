package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class FastApiDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long schoolId;
        private String targetDate;
        private List<String> reviewTexts;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String sentimentLabel;
        private Float sentimentScore;
        private Float sentimentConf;
        private List<String> aspectTags;
        private List<String> evidencePhrases;
        private Boolean issueFlags;
    }
}