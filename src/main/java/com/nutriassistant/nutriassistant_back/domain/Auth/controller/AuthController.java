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
    // 1. 아이디 찾기 (학생 / 영양사 분리)
    // =========================================================================

    // 학생 아이디 찾기 (이름 + 전화번호)
    @PostMapping("/student/find-id")
    public ResponseEntity<Map<String, String>> findStudentId(@RequestBody @Valid StudentFindIdRequest request) {
        String username = authService.findStudentId(request);
        return ResponseEntity.ok(Map.of("username", username));
    }

    // 영양사 아이디 찾기 (이름 + 이메일)
    @PostMapping("/dietitian/find-id")
    public ResponseEntity<Map<String, String>> findDietitianId(@RequestBody @Valid DietitianFindIdRequest request) {
        // 보안상 이메일로 전송하는 것이 좋지만, 기존 로직 유지를 위해 반환하거나
        // 서비스에서 이메일 전송 후 "이메일로 전송됨" 메시지를 리턴할 수 있습니다.
        // 여기서는 ID를 직접 반환하는 형태로 작성했습니다. (필요 시 수정 가능)
        String username = authService.findDietitianId(request);
        return ResponseEntity.ok(Map.of("username", username));
    }

    // =========================================================================
    // 2. 비밀번호 찾기 (학생 / 영양사 분리)
    // =========================================================================

    // 학생 비밀번호 찾기 (아이디 + 이름 + 전화번호 -> 임시 비번 발급)
    @PostMapping("/student/find-pw")
    public ResponseEntity<Map<String, String>> findStudentPw(@RequestBody @Valid StudentFindPasswordRequest request) {
        String tempPassword = authService.findStudentPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "임시 비밀번호가 발급되었습니다.",
                "temporaryPassword", tempPassword
        ));
    }

    // 영양사 비밀번호 찾기 (아이디 + 이름 + 이메일 -> 이메일 발송 or 임시 비번)
    @PostMapping("/dietitian/find-pw")
    public ResponseEntity<Map<String, String>> findDietitianPw(@RequestBody @Valid DietitianFindPasswordRequest request) {
        // 영양사는 이메일로 임시 비밀번호를 전송하는 로직이 서비스에 구현되어 있다고 가정합니다.
        // 만약 바로 보여주는 방식이라면 Student와 동일하게 tempPassword를 반환하면 됩니다.
        authService.findDietitianPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "입력하신 이메일로 임시 비밀번호가 전송되었습니다."
        ));
    }

    // =========================================================================
    // 이메일 중복체크
    // =========================================================================

    @GetMapping("/student/check-email")
    public ResponseEntity<Map<String, Boolean>> checkStudentEmail(
            @RequestParam String email) {
        boolean available = authService.checkStudentEmailAvailable(email);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @GetMapping("/dietitian/check-email")
    public ResponseEntity<Map<String, Boolean>> checkDietitianEmail(
            @RequestParam String email) {
        boolean available = authService.checkDietitianEmailAvailable(email);
        return ResponseEntity.ok(Map.of("available", available));
    }

    // =========================================================================
    // 3. 학생 (Student) 회원가입 / 로그인
    // =========================================================================
    @PostMapping("/signup/student")
    public ResponseEntity<SignUpResponse> signupStudent(@Valid @RequestBody SignUpRequest request) {
        // AuthService의 signupStudent 메서드를 호출하여 학생 계정을 생성합니다.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.signupStudent(request));
    }

    // =================================================================
    // 1. 학생 로그인
    // URL: POST /api/auth/login/student
    // =================================================================
    @PostMapping("/login/student")
    public ResponseEntity<?> loginStudent(@Valid @RequestBody LoginRequest request) {
        // AuthService의 학생 로그인 메서드 호출
        String token = authService.login(request);

        // 응답 (JSON 형식으로 토큰 반환)
        return ResponseEntity.ok().body(Map.of("accessToken", token));
    }

    // =========================================================================
    // 4. 영양사 (Dietitian) 회원가입 / 로그인
    // =========================================================================
    @PostMapping("/signup/dietitian")
    public ResponseEntity<DietitianSignUpResponse> signupDietitian(@Valid @RequestBody DietitianSignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signupDietitian(request));
    }

    // =================================================================
    // 2. 영양사 로그인
    // URL: POST /api/auth/login/dietitian
    // =================================================================
    @PostMapping("/login/dietitian")
    public ResponseEntity<?> loginDietitian(@Valid @RequestBody LoginRequest request) {
        // AuthService의 영양사 로그인 메서드 호출
        String token = authService.loginDietitian(request);

        // 응답
        return ResponseEntity.ok().body(Map.of("accessToken", token));
    }

    // =========================================================================
    // 5. 비밀번호 변경 (로그인 후)
    // =========================================================================

    // 학생 비밀번호 변경
    @PutMapping("/password/change/student")
    public ResponseEntity<String> changeStudentPassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody PasswordChangeRequest request) {

        authService.changeStudentPassword(user.getId(), request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    // 영양사 비밀번호 변경
    @PutMapping("/password/change/dietitian")
    public ResponseEntity<String> changeDietitianPassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody PasswordChangeRequest request) {

        authService.changeDietitianPassword(user.getId(), request);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    // =========================================================================
    // 6. 회원 탈퇴 (비밀번호 확인 필수)
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
    // 7. 로그아웃
    // =========================================================================
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }
}