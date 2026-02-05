package com.nutriassistant.nutriassistant_back.domain.School.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SchoolUpdateRequest {

    // --- 기본 정보 ---
    @JsonProperty("school_name")
    private String schoolName;

    @JsonProperty("school_code")
    private String schoolCode;

    @JsonProperty("region_code")
    private String regionCode;

    private String address;

    @JsonProperty("school_type")
    private String schoolType;

    // --- [추가] 운영 및 연락처 정보 ---

    @JsonProperty("phone")
    private String phone;        // 전화번호

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @JsonProperty("email")
    private String email;        // 이메일

    @JsonProperty("student_count")
    private Integer studentCount; // 급식 학생 수

    @JsonProperty("target_unit_price")
    private Integer targetUnitPrice; // 목표 단가

    @JsonProperty("max_unit_price")
    private Integer maxUnitPrice;    // 최대 단가

    @JsonProperty("operation_rules")
    private String operationRules;   // 운영 규칙 (배식 시간, 특이사항 등)

    @JsonProperty("cook_workers")
    private Integer cookWorkers;     // 조리 종사자 수

    @JsonProperty("kitchen_equipment")
    private String kitchenEquipment; // 주방 기구 현황
}