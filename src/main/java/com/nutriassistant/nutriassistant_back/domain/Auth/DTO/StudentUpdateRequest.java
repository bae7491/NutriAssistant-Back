package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 학생 정보 수정 요청 DTO
 *
 * 역할:
 * - 프론트엔드에서 학생 프로필 수정 시 보내는 JSON 데이터를 매핑합니다.
 * - 데이터의 유효성(Validation)을 1차적으로 검증합니다.
 *
 * 주의사항:
 * - 비밀번호 수정은 별도의 DTO와 로직으로 분리되어 있습니다.
 * - 이 DTO는 이름, 전화번호, 학년, 반, 알레르기 정보만 다룹니다.
 */
public class StudentUpdateRequest {

    /**
     * 학생 이름
     * - 필수 입력 (공백 불가)
     */
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    /**
     * 전화번호
     * - 필수 입력 (공백 불가)
     * - 추후 @Pattern을 사용하여 정규식 검증을 추가할 수 있습니다. (예: 010-xxxx-xxxx)
     */
    @NotBlank(message = "전화번호는 필수 입력 값입니다.")
    private String phone;

    /**
     * 학년
     * - 필수 입력
     * - 최소값 1 이상
     */
    @NotNull(message = "학년은 필수 입력 값입니다.")
    @Min(value = 1, message = "학년은 1 이상이어야 합니다.")
    private Integer grade;

    /**
     * 반 (Class Number)
     * - 필수 입력
     * - 최소값 1 이상
     * - JSON 키 매핑: "class_no" (Snake Case)
     */
    @NotNull(message = "반 정보는 필수 입력 값입니다.")
    @Min(value = 1, message = "반은 1 이상이어야 합니다.")
    @JsonProperty("class_no")
    private Integer classNo;

    /**
     * 알레르기 코드 리스트
     * - 선택 입력 (Null 가능)
     * - JSON 키 매핑: "allergy_codes" (Snake Case 권장)
     * - 예: [1, 3, 5] -> 1번(난류), 3번(메밀), 5번(대두) 알레르기 보유
     */
    @JsonProperty("allergy_codes")
    private List<Integer> allergyCodes;

    // 기본 생성자
    public StudentUpdateRequest() {}

    // =========================================================================
    // Getters & Setters
    // =========================================================================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public Integer getClassNo() { return classNo; }
    public void setClassNo(Integer classNo) { this.classNo = classNo; }

    // [수정] 필드가 추가되었으므로 정상적으로 Getter/Setter 동작
    public List<Integer> getAllergyCodes() { return allergyCodes; }
    public void setAllergyCodes(List<Integer> allergyCodes) { this.allergyCodes = allergyCodes; }
}