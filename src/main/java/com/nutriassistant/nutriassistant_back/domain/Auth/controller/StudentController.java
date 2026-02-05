package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.StudentProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.StudentUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.AuthService;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.StudentService; // StudentService 추가 필요
import com.nutriassistant.nutriassistant_back.global.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final AuthService authService;
    private final StudentService studentService; // 조회 로직을 위해 주입

    /*
     * [내 정보 조회]
     * 경로: GET /api/student/me
     * 토큰에서 ID를 추출하여 본인의 프로필 정보를 조회합니다.
     */
    @GetMapping("/me")
    public ResponseEntity<StudentProfileResponse> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long studentId = userDetails.getId();
        return ResponseEntity.ok(studentService.getStudentProfile(studentId));
    }

    /*
     * [학생 프로필 수정]
     * 경로: PUT /api/student/me
     * URL에 ID를 노출하지 않고, 토큰(UserDetails)에서 ID를 추출하여 본인 정보만 수정합니다.
     */
    @PutMapping("/me")
    public ResponseEntity<SignUpResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StudentUpdateRequest request) {

        Long studentId = userDetails.getId();
        return ResponseEntity.ok(authService.updateStudentProfile(studentId, request));
    }

    /*
     * [학생 비밀번호 변경]
     * 경로: PUT /api/student/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {

        Long studentId = userDetails.getId();
        authService.changeStudentPassword(studentId, request);
        return ResponseEntity.noContent().build();
    }
}