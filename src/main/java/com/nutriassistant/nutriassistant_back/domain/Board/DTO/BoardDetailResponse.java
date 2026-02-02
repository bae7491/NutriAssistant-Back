package com.nutriassistant.nutriassistant_back.domain.Board.DTO;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardDetailResponse {

    private Long id;
    private Long schoolId;
    private String category;
    private String title;
    private String content;
    private Long authorId;
    private String authorType;
    private String authorName;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttachmentInfo> attachments;
    private Boolean isMine;
    private Boolean isEditable;

    @Getter
    @Builder
    public static class AttachmentInfo {
        private Long fileId;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
    }
}
