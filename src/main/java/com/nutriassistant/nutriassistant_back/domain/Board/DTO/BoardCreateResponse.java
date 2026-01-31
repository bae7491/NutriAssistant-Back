package com.nutriassistant.nutriassistant_back.domain.Board.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardCreateResponse {

    private Long id;

    private Long schoolId;

    private String category;

    private String title;

    private String content;

    private Long authorId;

    private String authorType;

    private Integer viewCount;

    private List<AttachmentResponse> attachments;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Getter
    @Builder
    public static class AttachmentResponse {
        private Long id;
        private String relatedType;
        private Long relatedId;
        private String fileName;
        private String s3Path;
        private String fileType;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
