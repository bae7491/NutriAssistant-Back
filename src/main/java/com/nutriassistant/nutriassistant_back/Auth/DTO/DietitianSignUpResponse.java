package com.nutriassistant.nutriassistant_back.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.Auth.entity.School;

import java.time.LocalDateTime;

public class DietitianSignUpResponse {

    @JsonProperty("dietitian_id")
    private Long dietitianId;

    private String username;
    private String name;
    private String phone;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    private SchoolResponse school;

    public DietitianSignUpResponse() {}

    public DietitianSignUpResponse(Dietitian dietitian, School school) {
        this.dietitianId = dietitian.getId();
        this.username = dietitian.getUsername();
        this.name = dietitian.getName();
        this.phone = dietitian.getPhone();
        this.createdAt = dietitian.getCreatedAt();
        this.updatedAt = dietitian.getUpdatedAt();
        this.school = new SchoolResponse(school);
    }

    public Long getDietitianId() { return dietitianId; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public SchoolResponse getSchool() { return school; }
}
