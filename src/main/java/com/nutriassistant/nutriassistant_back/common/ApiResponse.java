package com.nutriassistant.nutriassistant_back.common;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String status;
    private final String message;
    private final T data;
    private final ErrorDetails details;

    private ApiResponse(String status, String message, T data, ErrorDetails details) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.details = details;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null, null);
    }

    public static <T> ApiResponse<T> error(String message, ErrorDetails details) {
        return new ApiResponse<>("error", message, null, details);
    }

    @Getter
    public static class ErrorDetails {
        private final String field;
        private final String reason;
        private final String errorId;

        public ErrorDetails(String field, String reason) {
            this.field = field;
            this.reason = reason;
            this.errorId = null;
        }

        public ErrorDetails(String errorId) {
            this.field = null;
            this.reason = null;
            this.errorId = errorId;
        }
    }
}