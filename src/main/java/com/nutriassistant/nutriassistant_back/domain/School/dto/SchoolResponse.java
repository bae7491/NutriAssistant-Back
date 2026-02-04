package com.nutriassistant.nutriassistant_back.domain.School.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [학교 정보 응답 DTO]
 * - 역할: 클라이언트(프론트엔드)에게 학교 정보를 반환할 때 사용하는 객체입니다.
 * - 사용처 1: 나이스(NEIS) API 검색 결과 반환
 * - 사용처 2: DB에 저장된 학교 상세 정보 조회 (마이페이지, 학생의 학교 정보 조회 등)
 */
@Getter
@Builder // Controller 및 서비스에서 빌더 패턴(.builder().build()) 사용을 위해 필수
@NoArgsConstructor // JSON 파싱(Jackson) 및 JPA 사용 시 기본 생성자 필수
@AllArgsConstructor // Builder 패턴 사용 시 전체 인자 생성자 필수
public class SchoolResponse {

    // =========================================================================
    // [1] 나이스(NEIS) API 검색 및 기본 식별 정보
    // =========================================================================

    @JsonProperty("school_name")
    private String schoolName; // 학교 이름 (예: 부산소프트웨어마이스터고)

    @JsonProperty("region_code")
    private String regionCode; // 시도교육청코드 (예: C10) - 나이스 API 필수값

    @JsonProperty("school_code")
    private String schoolCode; // 표준학교코드 (예: 7150658) - 나이스 API 필수값

    @JsonProperty("address")
    private String address;    // 학교 주소 (나이스 API 제공)

    // =========================================================================
    // [2] 서비스 내부 DB 관리 정보 (상세 조회 시 사용)
    // =========================================================================

    @JsonProperty("school_id")
    private Long schoolId;     // DB PK (서버 내부 관리용 ID)

    @JsonProperty("dietitian_id")
    private Long dietitianId;  // 담당 영양사 ID (영양사가 학교를 등록했을 경우 매핑됨)

    @JsonProperty("school_type")
    private String schoolType; // 학교 급 (초등학교/중학교/고등학교/특수학교 등)

    @JsonProperty("phone")     // [수정] JSON 필드명 명시
    private String phone;      // 학교 전화번호

    @JsonProperty("email")     // [수정] JSON 필드명 명시
    private String email;      // 학교 대표 이메일

    @JsonProperty("student_count")
    private Integer studentCount; // 급식 대상 학생 수

    @JsonProperty("target_unit_price")
    private Integer targetUnitPrice; // 목표 급식 단가

    @JsonProperty("max_unit_price")
    private Integer maxUnitPrice;    // 최대 허용 단가

    @JsonProperty("operation_rules")
    private String operationRules;   // 식당 운영 규칙 및 안내사항

    @JsonProperty("cook_workers")
    private Integer cookWorkers;     // 조리 실무사(종사자) 수

    @JsonProperty("kitchen_equipment")
    private String kitchenEquipment; // 보유 주방 기기 목록 (오븐, 식기세척기 등)

    // =========================================================================
    // [3] 메타 데이터
    // =========================================================================

    @JsonProperty("created_at")
    private LocalDateTime createdAt; // 데이터 생성일 (학교 등록일)

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt; // 데이터 수정일 (정보 업데이트일)

    /**
     * [Entity -> DTO 변환 생성자]
     * DB에서 조회한 School 엔티티(Entity)를 응답 객체(DTO)로 변환합니다.
     * 엔티티의 모든 정보를 DTO로 복사하여 프론트엔드에 전달할 준비를 합니다.
     *
     * @param school DB에서 조회된 School 엔티티
     */
    public SchoolResponse(School school) {
        // 1. 기본 정보 매핑 (필수)
        this.schoolId = school.getId();
        this.schoolName = school.getSchoolName();
        this.regionCode = school.getRegionCode();
        this.schoolCode = school.getSchoolCode();
        this.address = school.getAddress();

        // 2. 담당 영양사 정보 매핑 (존재할 경우에만 ID 추출)
        if (school.getDietitian() != null) {
            this.dietitianId = school.getDietitian().getId();
        }

        // 3. [수정] 상세 운영 정보 매핑 (주석 해제 완료)
        // School 엔티티에 저장된 운영 정보를 DTO에 담습니다.
        this.schoolType = school.getSchoolType();
        this.phone = school.getPhone();
        this.email = school.getEmail();
        this.studentCount = school.getStudentCount();
        this.targetUnitPrice = school.getTargetUnitPrice();
        this.maxUnitPrice = school.getMaxUnitPrice();
        this.operationRules = school.getOperationRules();
        this.cookWorkers = school.getCookWorkers();
        this.kitchenEquipment = school.getKitchenEquipment();

        // 4. 날짜 정보 매핑
        this.createdAt = school.getCreatedAt();
        this.updatedAt = school.getUpdatedAt();
    }
}