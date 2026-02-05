package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.*;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =========================================================================
    // 1. 공통 기능 (아이디 / 비밀번호 찾기)
    // =========================================================================

    // 아이디 찾기
    @PostMapping("/find-id")
    public ResponseEntity<Map<String, String>> findId(@RequestBody FindIdRequest request) {
        String username = authService.findUsername(request);
        return ResponseEntity.ok(Map.of("username", username));
    }

    // 비밀번호 찾기 (임시 비밀번호 발급)
    @PostMapping("/find-pw")
    public ResponseEntity<Map<String, String>> findPw(@RequestBody FindPasswordRequest request) {
        String tempPassword = authService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "임시 비밀번호가 발급되었습니다.",
                "temporaryPassword", tempPassword
        ));
    }

    // =========================================================================
    // 2. 학생 (Student)
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
    // 3. 영양사 (Dietitian)
    // =========================================================================
    @PostMapping("/signup/dietitian")
    public ResponseEntity<DietitianSignUpResponse> signupDietitian(@Valid @RequestBody DietitianSignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupDietitian(request));
    }

    @PostMapping("/login/dietitian")
    public ResponseEntity<Map<String, String>> loginDietitian(@RequestBody LoginRequest request) {
        String token = authService.loginDietitian(request);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }
}