package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.StudentUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.AuthService;
import com.nutriassistant.nutriassistant_back.global.jwt.CustomUserDetails; // ※ 프로젝트의 실제 UserDetails 클래스 import 필요
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

    /**
     * ✅ 학생 프로필 수정
     * 경로: PUT /api/student/me
     * 설명: URL에 ID를 노출하지 않고, 토큰(UserDetails)에서 ID를 추출하여 본인 정보만 수정
     */
    @PutMapping("/me")
    public ResponseEntity<SignUpResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 토큰에서 사용자 정보 추출
            @Valid @RequestBody StudentUpdateRequest request) {

        Long studentId = userDetails.getId(); // CustomUserDetails에 getId()가 있다고 가정
        return ResponseEntity.ok(authService.updateStudentProfile(studentId, request));
    }

    /**
     * ✅ 학생 비밀번호 변경
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