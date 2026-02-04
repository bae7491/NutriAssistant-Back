package com.nutriassistant.nutriassistant_back.domain.School.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeisSchoolResponse {

    // 나이스 API는 최상위에 "schoolInfo"라는 키로 데이터를 감싸서 줍니다.
    @JsonProperty("schoolInfo")
    private List<SchoolInfoWrapper> schoolInfo;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchoolInfoWrapper {
        // 성공 여부(head)와 실제 데이터(row)가 리스트 안에 섞여 들어오는 구조입니다.
        private List<SchoolRow> row;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchoolRow {

        // 1. 시도교육청코드 (Region Code) -> Entity의 regionCode
        @JsonProperty("ATPT_OFCDC_SC_CODE")
        private String regionCode;

        // 2. 표준학교코드 (School Code) -> Entity의 schoolCode
        @JsonProperty("SD_SCHUL_CODE")
        private String schoolCode;

        // 3. 학교명 (School Name) -> Entity의 schoolName
        @JsonProperty("SCHUL_NM")
        private String schoolName;

        // 4. 주소 (Address) -> Entity의 address
        @JsonProperty("ORG_RDNMA")
        private String address;
    }
}