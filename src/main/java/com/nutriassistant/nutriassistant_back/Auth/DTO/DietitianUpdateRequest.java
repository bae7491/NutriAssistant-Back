package com.nutriassistant.nutriassistant_back.Auth.DTO;

import jakarta.validation.constraints.NotBlank;

public class DietitianUpdateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    public DietitianUpdateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
