package com.nutriassistant.nutriassistant_back.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeRequest {

    @NotBlank
    @JsonProperty("current_pw")
    private String currentPw;

    @NotBlank
    @Size(min = 8, max = 72)
    @JsonProperty("new_pw")
    private String newPw;

    public PasswordChangeRequest() {}

    public String getCurrentPw() { return currentPw; }
    public void setCurrentPw(String currentPw) { this.currentPw = currentPw; }

    public String getNewPw() { return newPw; }
    public void setNewPw(String newPw) { this.newPw = newPw; }
}
