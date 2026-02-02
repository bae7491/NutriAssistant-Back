package com.nutriassistant.nutriassistant_back.domain.Auth.util;

public final class PhoneNumberUtil {
    private PhoneNumberUtil() {}

    public enum Mode {
        MOBILE_ONLY,   // 학생/영양사
        KR_GENERAL     // 학교(지역번호/070/대표번호 1588 등)
    }

    public static String normalizeToDigits(String input, Mode mode) {
        if (input == null) return null;

        // 내선 등 뒤쪽 텍스트가 섞일 가능성 일부 제거(선택)
        String base = input.replaceAll("(?i)(내선|ext\\.?|extension|x|#).*$", "");

        String digits = base.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;

        // +82/82 -> 0... 보정
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }

        if (mode == Mode.MOBILE_ONLY) {
            // 휴대폰만: 01x + 7~8자리
            if (!digits.matches("^01[0-9]\\d{7,8}$")) {
                throw new IllegalArgumentException("휴대폰 번호 형식이 올바르지 않습니다: " + input);
            }
            return digits;
        }

        // 학교/기관 일반전화 + 대표번호
        boolean normalTel  = digits.matches("^0\\d{8,10}$");       // 9~11자리 (02/031/070/010 등)
        boolean serviceTel = digits.matches("^(15|16|18)\\d{6}$");  // 1588/1577/1600/1800 등 8자리

        if (!(normalTel || serviceTel)) {
            throw new IllegalArgumentException("전화번호 형식이 올바르지 않습니다: " + input);
        }
        return digits;
    }
}
