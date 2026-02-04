package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 비밀번호 변경 요청 DTO
 *
 * 역할:
 * - 마이페이지 등에서 비밀번호를 변경할 때 사용합니다.
 * - AuthService 및 DietitianService에서 사용됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeRequest {

    // [수정] currentPw -> currentPassword 로 변경
    // AuthService에서 request.getCurrentPassword()를 호출하기 위함
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    @JsonProperty("current_password") // JSON 요청 키: "current_password"
    private String currentPassword;

    // [수정] newPw -> newPassword 로 변경
    // AuthService에서 request.getNewPassword()를 호출하기 위함
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다.") // 유효성 검사 기준 통일
    @JsonProperty("new_password") // JSON 요청 키: "new_password"
    private String newPassword;

    // 🔴 [삭제됨] 컴파일 에러를 유발하던 빈 메서드 삭제
    // public CharSequence getCurrentPw() {}
    // public CharSequence getNewPw() {}
}