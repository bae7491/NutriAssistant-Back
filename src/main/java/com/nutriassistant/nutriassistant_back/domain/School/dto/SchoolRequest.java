package com.nutriassistant.nutriassistant_back.domain.School.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolRequest {

    // [1] 나이스(NEIS) 연동 필수 정보
    @JsonProperty("school_name")
    private String schoolName;

    @JsonProperty("region_code")
    private String regionCode;

    @JsonProperty("school_code")
    private String schoolCode;

    @JsonProperty("address")
    private String address;

    // [2] 학교 운영 상세 정보
    @JsonProperty("school_type")
    private String schoolType;

    @JsonProperty("phone") // [수정] 어노테이션 추가
    private String phone;

    @JsonProperty("email") // [수정] 어노테이션 추가
    private String email;

    @JsonProperty("student_count")
    private Integer studentCount;

    @JsonProperty("target_unit_price")
    private Integer targetUnitPrice;

    @JsonProperty("max_unit_price")
    private Integer maxUnitPrice;

    @JsonProperty("operation_rules")
    private String operationRules;

    @JsonProperty("cook_workers")
    private Integer cookWorkers;

    @JsonProperty("kitchen_equipment")
    private String kitchenEquipment;
}