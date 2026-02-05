package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.*;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.AuthService;
import com.nutriassistant.nutriassistant_back.global.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // =========================================================================
    // 4. 비밀번호 변경 (로그인 후)
    // =========================================================================

    // 학생 비밀번호 변경
    @PutMapping("/password/change/student")
    public ResponseEntity<String> changeStudentPassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody PasswordChangeRequest request) {

        authService.changeStudentPassword(user.getId(), request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    // [추가] 영양사 비밀번호 변경
    @PutMapping("/password/change/dietitian")
    public ResponseEntity<String> changeDietitianPassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody PasswordChangeRequest request) {

        authService.changeDietitianPassword(user.getId(), request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    // =========================================================================
    // 5. 회원 탈퇴 (비밀번호 확인 필수)
    // =========================================================================

    // 학생 탈퇴
    @DeleteMapping("/withdraw/student")
    public ResponseEntity<String> withdrawStudent(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody WithdrawalRequest request) {

        authService.withdrawStudent(user.getId(), request.getPw());
        return ResponseEntity.ok("성공적으로 탈퇴되었습니다.");
    }

    // 영양사 탈퇴
    @DeleteMapping("/withdraw/dietitian")
    public ResponseEntity<String> withdrawDietitian(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody WithdrawalRequest request) {

        authService.withdrawDietitian(user.getId(), request.getPw());
        return ResponseEntity.ok("성공적으로 탈퇴되었습니다.");
    }

    // =========================================================================
    // 6. 로그아웃
    // =========================================================================
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}