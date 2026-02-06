package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [회원 탈퇴 요청 DTO]
 * 탈퇴 시 본인 확인을 위해 비밀번호를 입력받습니다.
 */
@Getter
@Setter
@NoArgsConstructor
public class WithdrawalRequest {

    @NotBlank(message = "탈퇴를 진행하시려면 비밀번호를 입력해주세요.")
    private String password;

    /**
     * 기존 코드(pw)와의 호환성을 위한 메서드
     * 서비스 단에서 request.getPw()를 호출하고 있다면 이 메서드가 필요합니다.
     */
    public String getPw() {
        return this.password;
    }
}