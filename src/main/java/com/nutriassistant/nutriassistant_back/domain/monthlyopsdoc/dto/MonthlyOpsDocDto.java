package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MonthlyOpsDocDto {

    // [수정] 생성 요청 DTO
    // 프론트엔드는 학교ID와 연/월만 보내면 됩니다. (내용과 파일은 백엔드가 생성)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private Long school_id;
        private String title;
        private Integer year;
        private Integer month;
        // report_content 제거됨 (서버 생성)
        // files 제거됨 (서버 생성)
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long school_id;
        private String title;
        private Integer year;
        private Integer month;
        private String status;
        private Map<String, Object> report_content;
        private LocalDateTime created_at;
        private List<FileResponse> files;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {
        private List<Response> reports;
        private Pagination pagination;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Pagination {
        private int current_page;
        private int total_pages;
        private long total_items;
        private int page_size;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileResponse {
        private Long id;
        private String file_name;
        private String file_type;
        private String s3_path;
        private LocalDateTime created_at;
    }
}