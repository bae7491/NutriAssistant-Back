package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [영양사 프로필 응답 DTO]
 * - 로그인 후 내 정보 조회(마이페이지) 시 반환되는 객체입니다.
 * - 영양사 개인 정보와 소속된 학교 정보를 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietitianProfileResponse {

    @JsonProperty("dietitian_id")
    private Long dietitianId; // 영양사 고유 ID (PK)

    private String username;  // 로그인 아이디
    private String name;      // 영양사 실명
    private String email;     // 이메일
    private String phone;     // 전화번호

    @JsonProperty("created_at")
    private LocalDateTime createdAt; // 가입 일시

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt; // 정보 수정 일시

    /**
     * 영양사가 소속된 학교 정보
     * - 학교가 아직 등록되지 않았거나 승인 대기 중일 경우 null일 수 있습니다.
     */
    @JsonProperty("school")
    private SchoolResponse school;

    /**
     * [Entity -> DTO 변환 생성자]
     * - 영양사(Dietitian) 엔티티와 학교(School) 엔티티를 받아 응답 객체를 생성합니다.
     * - 학교 정보가 없을 경우(null) 안전하게 처리합니다.
     */
    public DietitianProfileResponse(Dietitian dietitian, School school) {
        this.dietitianId = dietitian.getId();
        this.username = dietitian.getUsername();
        this.name = dietitian.getName();

        // [수정 완료] 이메일 매핑 코드 추가
        this.email = dietitian.getEmail();

        this.phone = dietitian.getPhone();
        this.createdAt = dietitian.getCreatedAt();
        this.updatedAt = dietitian.getUpdatedAt();

        // 학교 정보가 존재하면 SchoolResponse DTO로 변환하고, 없으면 null 처리
        this.school = (school != null) ? new SchoolResponse(school) : null;
    }
}