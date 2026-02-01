package com.nutriassistant.nutriassistant_back.Auth.controller;

import com.nutriassistant.nutriassistant_back.Auth.DTO.SignUpRequest;
import com.nutriassistant.nutriassistant_back.Auth.DTO.SignUpResponse;
import com.nutriassistant.nutriassistant_back.Auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupStudent(request));
    }
}
