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
 * [영양사 회원가입 응답 DTO]
 * - 회원가입 API 호출 성공 시 반환되는 데이터입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietitianSignUpResponse {

    @JsonProperty("dietitian_id")
    private Long dietitianId; // 생성된 영양사 ID

    private String username;  // 가입된 아이디
    private String name;      // 가입된 이름
    private String phone;     // 가입된 전화번호

    @JsonProperty("created_at")
    private LocalDateTime createdAt; // 가입 시간

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt; // 수정 시간 (가입 시점엔 생성 시간과 동일)

    /**
     * 가입 시 등록한 학교 정보
     * - 회원가입과 동시에 학교를 등록하는 로직일 경우 포함됩니다.
     */
    @JsonProperty("school")
    private SchoolResponse school;

    /**
     * [Entity -> DTO 변환 생성자]
     */
    public DietitianSignUpResponse(Dietitian dietitian, School school) {
        this.dietitianId = dietitian.getId();
        this.username = dietitian.getUsername();
        this.name = dietitian.getName();
        this.phone = dietitian.getPhone();
        this.createdAt = dietitian.getCreatedAt();
        this.updatedAt = dietitian.getUpdatedAt();

        // 학교 엔티티가 있다면 DTO로 변환하여 할당
        if (school != null) {
            this.school = new SchoolResponse(school);
        } else {
            this.school = null;
        }
    }
}