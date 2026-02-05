package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawalRequest {
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String pw;
}