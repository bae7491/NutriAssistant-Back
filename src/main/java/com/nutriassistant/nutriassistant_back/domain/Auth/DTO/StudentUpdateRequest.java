package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 학생 정보 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentUpdateRequest {

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.")
    private String phone;

    @NotNull(message = "학년은 필수 입력 값입니다.")
    @Min(value = 1, message = "학년은 1 이상이어야 합니다.")
    private Integer grade;

    @NotNull(message = "반 정보는 필수 입력 값입니다.")
    @Min(value = 1, message = "반은 1 이상이어야 합니다.")
    @JsonProperty("class_no")
    private Integer classNo;

    @JsonProperty("allergy_codes")
    private List<Integer> allergyCodes;
}