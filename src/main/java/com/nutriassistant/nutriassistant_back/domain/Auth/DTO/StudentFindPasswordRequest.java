package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentFindPasswordRequest {

    // 학생은 아이디가 이메일 형식이므로 프론트엔드에서 email로 보냅니다.
    // (Service에서는 이를 username으로 사용합니다.)
    @NotBlank(message = "아이디(이메일)를 입력해주세요.")
    private String email;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    // [다시 추가됨] 본인 확인을 위한 전화번호
    @NotBlank(message = "전화번호를 입력해주세요.")
    private String phone;
}