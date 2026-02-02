package com.nutriassistant.nutriassistant_back.domain.Board.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BoardUpdateRequest {

    private String category;

    private String title;

    private String content;

    private List<AttachmentRequest> attachments;

    @Getter
    @Setter
    public static class AttachmentRequest {
        private String fileName;
        private String s3Path;
        private String fileType;
    }
}
