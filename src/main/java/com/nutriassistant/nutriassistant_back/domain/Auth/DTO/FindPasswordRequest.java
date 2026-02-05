package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindPasswordRequest {
    private String username;
    private String name;
    private String phone;
}