package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.StudentProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.StudentUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.WithdrawalRequest; // WithdrawalRequest DTO 추가 필요
import com.nutriassistant.nutriassistant_back.domain.Auth.service.AuthService;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.StudentService;
import com.nutriassistant.nutriassistant_back.global.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그 기록용
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final AuthService authService;
    private final StudentService studentService;

    /**
     * [내 정보 조회]
     * 경로: GET /api/student/me
     */
    @GetMapping("/me")
    public ResponseEntity<StudentProfileResponse> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long studentId = userDetails.getId();
        log.info("🔍 학생 프로필 조회 요청: ID={}", studentId);
        return ResponseEntity.ok(studentService.getStudentProfile(studentId));
    }

    /**
     * [학생 프로필 수정]
     * 경로: PUT /api/student/me
     */
    @PutMapping("/me")
    public ResponseEntity<SignUpResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StudentUpdateRequest request) {

        Long studentId = userDetails.getId();
        log.info("✏️ 학생 프로필 수정 요청: ID={}", studentId);
        return ResponseEntity.ok(authService.updateStudentProfile(studentId, request));
    }

    /**
     * [학생 비밀번호 변경]
     * 경로: PUT /api/student/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {

        Long studentId = userDetails.getId();
        log.info("🔒 학생 비밀번호 변경 요청: ID={}", studentId);
        authService.changeStudentPassword(studentId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * [학생 회원 탈퇴]
     * 경로: POST /api/student/me/withdraw
     * 보안을 위해 DELETE가 아닌 POST를 사용하며, 비밀번호 확인을 거칩니다.
     */
    @PostMapping("/me/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WithdrawalRequest request) {

        Long studentId = userDetails.getId();
        log.info("🚪 학생 회원 탈퇴 요청: ID={}", studentId);

        // AuthService에 구현된 탈퇴 로직 호출 (비밀번호 검증 포함)
        authService.withdrawStudent(studentId, request.getPassword());

        return ResponseEntity.noContent().build();
    }
}