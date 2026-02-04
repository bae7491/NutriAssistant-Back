package com.nutriassistant.nutriassistant_back.domain.File.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FileUploadResponse {

    private Long id;

    @JsonProperty("related_type")
    private String relatedType;

    @JsonProperty("related_id")
    private Long relatedId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_type")
    private String fileType;

    @JsonProperty("file_size")
    private Long fileSize;

    @JsonProperty("s3_path")
    private String s3Path;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
