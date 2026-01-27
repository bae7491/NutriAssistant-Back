package com.nutriassistant.nutriassistant_back.MealPlan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(
        name = "meal_plan_menu",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meal_plan_menu_plan_date_type",
                columnNames = {"meal_plan_id", "menu_date", "meal_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class MealPlanMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(name = "menu_date", nullable = false)
    private LocalDate menuDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    // --- 메뉴 구성 필드 ---
    private String rice;
    private String soup;
    private String main1;
    private String main2;
    private String side;
    private String kimchi;
    private String dessert;

    // [수정] 영양소 계산을 위해 Integer -> Double로 변경
    // (FoodDictionary의 값이 double이므로 합산 시 소수점이 발생할 수 있음)
    private Double kcal;
    private Double carb;
    private Double prot;
    private Double fat;

    private Integer cost; // 비용은 보통 정수(원 단위)로 관리

    @Column(name = "raw_menus_json", columnDefinition = "tinytext")
    private String rawMenusJson;

    @Column(name = "ai_comment", length = 1000)
    private String aiComment;

    // =================================================================
    // 편의 메서드
    // =================================================================

    /**
     * [수정] AI 자동 대체 또는 수동 수정 시 메뉴 업데이트
     * 리스트 순서대로 엔티티의 필드(rice, soup...)에 매핑합니다.
     * @param newMenus 알레르기 정보가 포함된 메뉴 리스트
     */
    public void updateMenus(List<String> newMenus) {
        // [수정] null이 들어오면 모든 필드를 초기화(null)하도록 로직 보완
        if (newMenus == null || newMenus.isEmpty()) {
            this.rice = null;
            this.soup = null;
            this.main1 = null;
            this.main2 = null;
            this.side = null;
            this.kimchi = null;
            this.dessert = null;
            return;
        }

        // 리스트 인덱스에 맞춰 필드 할당 (데이터가 없으면 null 들어감)
        this.rice    = getSafe(newMenus, 0);
        this.soup    = getSafe(newMenus, 1);
        this.main1   = getSafe(newMenus, 2);
        this.main2   = getSafe(newMenus, 3);
        this.side    = getSafe(newMenus, 4);
        this.kimchi  = getSafe(newMenus, 5);
        this.dessert = getSafe(newMenus, 6);
    }

    /**
     * [추가] rawMenus 업데이트 메서드
     * Service에서 JSON 변환을 수행하지 않고 엔티티에 위임할 때 사용합니다.
     * @param rawMenus 비용 계산용 순수 메뉴명 리스트
     * @param objectMapper JSON 변환용 ObjectMapper
     */
    public void updateRawMenus(List<String> rawMenus, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        if (rawMenus == null || rawMenus.isEmpty()) {
            // [수정] 빈 리스트가 오면 null 대신 빈 배열 "[]"로 저장하는 것이 안전할 수 있음
            // (프론트엔드 처리에 따라 다르지만, 보통 빈 배열 문자열이 파싱 오류가 적음)
            this.rawMenusJson = "[]";
            return;
        }

        try {
            this.rawMenusJson = objectMapper.writeValueAsString(rawMenus);
        } catch (Exception e) {
            System.err.println("❌ rawMenus JSON 변환 실패: " + e.getMessage());
            this.rawMenusJson = "[]"; // 실패 시 빈 배열 저장
        }
    }

    // [기존 유지] 메뉴 전체를 문자열로 연결하여 반환 (로그용/히스토리용)
    public String getMenuString() {
        return Stream.of(rice, soup, main1, main2, side, kimchi, dessert)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    // [기존 유지] 안전하게 리스트 요소 가져오기
    private String getSafe(List<String> list, int index) {
        if (list != null && list.size() > index) {
            return list.get(index);
        }
        return null;
    }
}