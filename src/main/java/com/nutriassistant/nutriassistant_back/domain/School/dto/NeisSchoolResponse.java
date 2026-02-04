package com.nutriassistant.nutriassistant_back.domain.School.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // ★ Setter 추가 (Service에서 school_id를 넣기 위해 필수)
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeisSchoolResponse {

    @JsonProperty("schoolInfo")
    private List<SchoolInfoWrapper> schoolInfo;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchoolInfoWrapper {
        private List<SchoolRow> row;
    }

    @Getter
    @Setter // ★ Service에서 DB 조회 후 ID를 채워넣으려면 Setter가 필요합니다.
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchoolRow {

        // ▼▼▼ [핵심 수정] school_id 필드 복구 ▼▼▼
        // 나이스 API에는 없지만, 프론트엔드에게 "이 학교 등록됐어!"라고 알려주는 용도입니다.
        @JsonProperty("school_id")
        private Long schoolId;

        // 1. 시도교육청코드
        @JsonProperty("ATPT_OFCDC_SC_CODE")
        private String regionCode;

        // 2. 표준학교코드
        @JsonProperty("SD_SCHUL_CODE")
        private String schoolCode;

        // 3. 학교명
        @JsonProperty("SCHUL_NM")
        private String schoolName;

        // 4. 주소
        @JsonProperty("ORG_RDNMA")
        private String address;

        // 5. 학교 종류 (초/중/고)
        @JsonProperty("SCHUL_KND_SC_NM")
        private String schoolType;

        // 6. 전화번호
        @JsonProperty("ORG_TELNO")
        private String phone;

        // 7. 남녀공학 구분 (필요시 사용)
        @JsonProperty("COEDU_SC_NM")
        private String coedu;
    }
}