package com.nutriassistant.nutriassistant_back.global;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

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

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ErrorDetails {
        private final Map<String, Object> properties = new LinkedHashMap<>();

        public ErrorDetails(String field, String reason) {
            if (field != null) properties.put("field", field);
            if (reason != null) properties.put("reason", reason);
        }

        public ErrorDetails(String errorId) {
            if (errorId != null) properties.put("error_id", errorId);
        }

        // 여러 key-value 쌍을 받는 생성자
        public ErrorDetails(String key1, String value1, String key2, String value2) {
            properties.put(key1, value1);
            properties.put(key2, value2);
        }

        public ErrorDetails(String key1, String value1, String key2, String value2, String key3, String value3) {
            properties.put(key1, value1);
            properties.put(key2, value2);
            properties.put(key3, value3);
        }

        @JsonAnyGetter
        public Map<String, Object> getProperties() {
            return properties;
        }
    }
}