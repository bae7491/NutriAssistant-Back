package com.nutriassistant.nutriassistant_back.domain.Auth.entity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 급식 알레르기 표기 표준(1~19)
 * - 코드(code): 1~19 정수
 * - 라벨(label): 한글 표기명
 *
 * 사용 예)
 * - 학생 알레르기: List<Integer> [1,5,6]
 * - DB 저장(문자열): "1,5,6"
 */
public enum Allergen {

    EGG(1, "난류(가금류)"),
    MILK(2, "우유"),
    BUCKWHEAT(3, "메밀"),
    PEANUT(4, "땅콩"),
    SOYBEAN(5, "대두"),
    WHEAT(6, "밀"),
    MACKEREL(7, "고등어"),
    CRAB(8, "게"),
    SHRIMP(9, "새우"),
    PORK(10, "돼지고기"),
    PEACH(11, "복숭아"),
    TOMATO(12, "토마토"),
    SULFITES(13, "아황산류"),
    WALNUT(14, "호두"),
    CHICKEN(15, "닭고기"),
    BEEF(16, "쇠고기"),
    SQUID(17, "오징어"),
    SHELLFISH(18, "조개류(굴·전복·홍합 포함)"),
    PINE_NUT(19, "잣");

    private final int code;
    private final String label;

    Allergen(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }

    // ------- Lookup -------
    private static final Map<Integer, Allergen> BY_CODE =
            Arrays.stream(values()).collect(Collectors.toMap(Allergen::getCode, a -> a));

    /** 코드 → enum (없으면 Optional.empty()) */
    public static Optional<Allergen> fromCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }

    /** 코드 → enum (없으면 예외) */
    public static Allergen fromCodeOrThrow(int code) {
        Allergen a = BY_CODE.get(code);
        if (a == null) throw new IllegalArgumentException("Unknown allergen code: " + code);
        return a;
    }

    // ------- Parse / Format helpers -------
    /**
     * "1,5,6" 같은 문자열을 [1,5,6]으로 파싱
     * - 공백/빈값은 무시
     * - 숫자 아닌 토큰은 무시(원하면 예외로 바꿔도 됨)
     */
    public static List<Integer> parseCodes(String csv) {
        if (csv == null || csv.trim().isEmpty()) return List.of();

        List<Integer> out = new ArrayList<>();
        for (String token : csv.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            try {
                out.add(Integer.parseInt(t));
            } catch (NumberFormatException ignored) {
                // 무시
            }
        }
        return out;
    }

    /** [1,5,6] → "1,5,6" */
    public static String toCsv(Collection<Integer> codes) {
        if (codes == null || codes.isEmpty()) return "";
        return codes.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    /** [1,5,6] → "난류(가금류), 대두, 밀" (표시용) */
    public static String toLabelString(Collection<Integer> codes) {
        if (codes == null || codes.isEmpty()) return "";
        return codes.stream()
                .filter(Objects::nonNull)
                .map(c -> fromCode(c).map(Allergen::getLabel).orElse("미정(" + c + ")"))
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
