package com.nutriassistant.nutriassistant_back.DTO;


import jakarta.persistence.Embeddable;

@Embeddable
public class MealEmbedded {

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

    // 기존 getter
    public String getRice() { return rice; }
    public String getSoup() { return soup; }
    public String getMain1() { return main1; }
    public String getMain2() { return main2; }
    public String getSide() { return side; }
    public String getKimchi() { return kimchi; }
    public Integer getKcal() { return kcal; }
    public Integer getProt() { return prot; }

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