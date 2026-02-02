package com.nutriassistant.nutriassistant_back.domain.Board.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardDeleteResponse {

    @JsonProperty("board_id")
    private Long boardId;

    private Boolean deleted;

    @JsonProperty("delete_type")
    private String deleteType;

    @JsonProperty("deleted_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;
}
