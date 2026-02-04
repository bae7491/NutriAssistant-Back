package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MonthlyOpsDocDto {

    // =================================================================
    // 1. 요청 (Request) DTO
    // =================================================================
    // [수정] 생성 요청 DTO
    // 프론트엔드는 연/월만 보내면 됩니다. (school_id는 JWT에서 추출, 내용과 파일은 백엔드가 생성)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        // 서비스 코드에서 request.getSchool_id()로 호출하므로 필드명 유지
        @JsonProperty("school_id")
        private Long school_id;

        // school_id는 JWT에서 자동 추출
        private String title;
        private Integer year;
        private Integer month;
    }

    // =================================================================
    // 2. 응답 (Response) DTO - 단건 조회
    // =================================================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;

        @JsonProperty("school_id")
        private Long school_id;

        private String title;
        private Integer year;
        private Integer month;
        private String status; // "COMPLETED", "PENDING" 등

        // AI 분석 결과 JSON이 Map으로 변환되어 들어감
        @JsonProperty("report_content")
        private Map<String, Object> report_content;

        @JsonProperty("created_at")
        private LocalDateTime created_at;

        // 첨부파일 리스트
        private List<FileResponse> files;
    }

    // =================================================================
    // 3. 응답 (Response) DTO - 목록 조회
    // =================================================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {
        private List<Response> reports;
        private Pagination pagination;
    }

    // =================================================================
    // 4. 공통 (Common) DTO - 파일, 페이지네이션
    // =================================================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileResponse {
        private Long id;

        @JsonProperty("file_name")
        private String file_name;

        @JsonProperty("file_type")
        private String file_type;

        @JsonProperty("s3_path")
        private String s3_path;

        @JsonProperty("created_at")
        private LocalDateTime created_at;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Pagination {
        @JsonProperty("current_page")
        private int current_page;

        @JsonProperty("total_pages")
        private int total_pages;

        @JsonProperty("total_items")
        private long total_items;

        @JsonProperty("page_size")
        private int page_size;
    }
}