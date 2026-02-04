package com.nutriassistant.nutriassistant_back.domain.School.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // Setter 추가
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
    @Setter // ★ 값을 나중에 채워넣기 위해 Setter 추가
    @NoArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SchoolRow {

        // ▼▼▼ [추가된 항목: 우리 DB의 PK] ▼▼▼
        // 나이스 API에서는 이 값을 안 줍니다. (기본값 null)
        // 백엔드에서 DB 조회를 한 뒤, 이미 등록된 학교라면 ID를 채워서 프론트로 보냅니다.
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

        // 5. 학교 종류
        @JsonProperty("SCHUL_KND_SC_NM")
        private String schoolType;

        // 6. 전화번호
        @JsonProperty("ORG_TELNO")
        private String phone;

        // 7. 홈페이지
        @JsonProperty("HMPG_ADRES")
        private String homepage;

        // 8. 남녀공학 구분
        @JsonProperty("COEDU_SC_NM")
        private String coedu;
    }
}