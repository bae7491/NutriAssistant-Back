package com.nutriassistant.nutriassistant_back.domain.Board.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardListResponse {

    @JsonProperty("current_page")
    private int currentPage;

    @JsonProperty("page_size")
    private int pageSize;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_items")
    private long totalItems;

    private List<BoardItem> items;

    @Getter
    @Builder
    public static class BoardItem {
        private Long id;

        @JsonProperty("school_id")
        private Long schoolId;

        private String category;
        private String title;
        private Long authorId;
        private String authorName;
        private String authorType;
        private Integer viewCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean hasAttachment;
    }
}
