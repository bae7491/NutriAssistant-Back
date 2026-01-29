package com.nutriassistant.nutriassistant_back.MealPlan.DTO;


import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class MealEmbedded {

    // 기존 getter
    private String rice;
    private String soup;
    private String main1;
    private String main2;
    private String side;
    private String kimchi;
    private Integer kcal;
    private Integer prot;

    protected MealEmbedded() {}

    public MealEmbedded(String rice, String soup, String main1, String main2,
                        String side, String kimchi, Integer kcal, Integer prot) {
        this.rice = rice;
        this.soup = soup;
        this.main1 = main1;
        this.main2 = main2;
        this.side = side;
        this.kimchi = kimchi;
        this.kcal = kcal;
        this.prot = prot;
    }

    // ✅ record 스타일 accessor 추가 (서비스의 m.rice()를 살림)
    public String rice() { return rice; }
    public String soup() { return soup; }
    public String main1() { return main1; }
    public String main2() { return main2; }
    public String side() { return side; }
    public String kimchi() { return kimchi; }
    public Integer kcal() { return kcal; }
    public Integer prot() { return prot; }
}