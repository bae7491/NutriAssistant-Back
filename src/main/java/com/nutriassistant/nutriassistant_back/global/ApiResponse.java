package com.nutriassistant.nutriassistant_back.global; // 패키지 경로 주의 (global.api)

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

    // 생성자
    private ApiResponse(String status, String message, T data, ErrorDetails details) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.details = details;
    }

    // ==========================================
    // 성공 응답 (Success)
    // ==========================================

    // 1. 데이터와 메시지를 모두 보내는 경우
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data, null);
    }

    // [수정됨] 2. 데이터만 보내는 경우 (Map, List, DTO 등 모두 가능)
    // 기존의 컴파일 에러가 나던 메서드를 이걸로 대체했습니다.
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", "요청이 성공했습니다.", data, null);
    }

    // ==========================================
    // 실패 응답 (Error)
    // ==========================================

    // 1. 메시지만 보내는 경우
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null, null);
    }

    // 2. 상세 에러 내용을 포함하는 경우
    public static <T> ApiResponse<T> error(String message, ErrorDetails details) {
        return new ApiResponse<>("error", message, null, details);
    }

    // ==========================================
    // 에러 상세 클래스 (Inner Class)
    // ==========================================
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