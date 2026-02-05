package com.nutriassistant.nutriassistant_back.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // 1. null 또는 빈 값 체크 (@NotBlank가 하겠지만 더블 체크)
        if (password == null || password.isBlank()) {
            return false;
        }

        // 2. 공백 포함 여부 체크 (스페이스바 금지)
        if (password.contains(" ")) {
            addMessage(context, "비밀번호에 공백을 포함할 수 없습니다.");
            return false;
        }

        // 3. 금지된 특수문자 체크: ( ) < > " ' ;
        if (password.matches(".*[()<>\";'].*")) {
            addMessage(context, "보안상 사용할 수 없는 특수문자(괄호, <>, \", ', ;)가 포함되어 있습니다.");
            return false;
        }

        // 4. [강화된 규칙] 영문, 숫자, 특수문자 각각 최소 1개 이상 포함 여부
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        // 허용된 특수문자(금지문자 및 영문/숫자 제외)가 있는지 확인
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9()<>\";'\\s].*");

        if (!hasLetter || !hasDigit || !hasSpecial) {
            addMessage(context, "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.");
            return false;
        }

        // 5. 길이 체크 (8자 ~ 20자)
        int len = password.length();
        if (len < 8 || len > 20) {
            addMessage(context, "비밀번호는 8자 이상 20자 이하여야 합니다.");
            return false;
        }

        return true;
    }

    // 에러 메시지 커스텀 유틸 메서드
    private void addMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}