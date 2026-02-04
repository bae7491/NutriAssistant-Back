package com.nutriassistant.nutriassistant_back.domain.Board.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BoardCreateRequest {

    @NotBlank(message = "카테고리는 필수 입력값입니다.")
    private String category;

    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String title;

    @NotBlank(message = "내용은 필수 입력값입니다.")
    private String content;

    // authorId, authorType은 JWT에서 자동 추출 (요청에서 제거)

    private List<AttachmentRequest> attachments;

    @Getter
    @Setter
    public static class AttachmentRequest {
        private String fileName;
        private String s3Path;
        private String fileType;
    }
}
