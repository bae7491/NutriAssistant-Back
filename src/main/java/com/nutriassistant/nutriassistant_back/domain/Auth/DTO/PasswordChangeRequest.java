package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
// [중요] ValidPassword가 위치한 실제 패키지 경로를 확인하고 맞춰주세요!
import com.nutriassistant.nutriassistant_back.global.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 비밀번호 변경 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeRequest {

    // 현재 비밀번호는 단순 일치 여부만 확인하므로 @ValidPassword 불필요
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    @JsonProperty("current_password")
    private String currentPassword;

    // [수정 완료] 새 비밀번호는 회원가입과 동일한 강력한 규칙 적용
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @ValidPassword  // ✅ 기존 @Size 대신 이 어노테이션 사용
    @JsonProperty("new_password")
    private String newPassword;
}