package com.nutriassistant.nutriassistant_back.Auth.controller;

import com.nutriassistant.nutriassistant_back.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.Auth.DTO.SignUpResponse;
import com.nutriassistant.nutriassistant_back.Auth.DTO.StudentUpdateRequest;
import com.nutriassistant.nutriassistant_back.Auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final AuthService authService;

    public StudentController(AuthService authService) {
        this.authService = authService;
    }

    // ✅ 학생 프로필 수정 (name, phone, grade, class_no만)
    @PutMapping("/{studentId}")
    public ResponseEntity<SignUpResponse> updateProfile(@PathVariable Long studentId,
                                                        @Valid @RequestBody StudentUpdateRequest request) {
        return ResponseEntity.ok(authService.updateStudentProfile(studentId, request));
    }

    // ✅ 학생 비밀번호 변경 (current_pw -> 검증 후 new_pw 저장)
    @PutMapping("/{studentId}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long studentId,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        authService.changeStudentPassword(studentId, request);
        return ResponseEntity.noContent().build();
    }
}
