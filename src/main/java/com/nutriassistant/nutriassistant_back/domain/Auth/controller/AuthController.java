package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.*;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =========================================================================
    // 학생 (Student)
    // =========================================================================

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupStudent(request));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }

    // =========================================================================
    // 영양사 (Dietitian)
    // =========================================================================

    // [영양사 회원가입] - 학교 정보 포함, 상세 응답 반환
    @PostMapping("/signup/dietitian")
    public ResponseEntity<DietitianSignUpResponse> signupDietitian(@Valid @RequestBody DietitianSignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupDietitian(request));
    }

    // [영양사 로그인]
    @PostMapping("/login/dietitian")
    public ResponseEntity<Map<String, String>> loginDietitian(@RequestBody LoginRequest request) {
        String token = authService.loginDietitian(request);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }
}