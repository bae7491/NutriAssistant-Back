package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.DietitianService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dietitian")
public class DietitianController {

    private final DietitianService dietitianService;

    public DietitianController(DietitianService dietitianService) {
        this.dietitianService = dietitianService;
    }

    // ✅ 영양사 회원가입 + 학교정보 기재(동시에)
    @PostMapping("/signup")
    public ResponseEntity<DietitianSignUpResponse> signup(@Valid @RequestBody DietitianSignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dietitianService.signupWithSchool(request));
    }

    // ✅ 영양사 프로필 수정 (name, phone만)
    @PutMapping("/{dietitianId}")
    public ResponseEntity<DietitianProfileResponse> updateProfile(@PathVariable Long dietitianId,
                                                                  @Valid @RequestBody DietitianUpdateRequest request) {
        return ResponseEntity.ok(dietitianService.updateDietitianProfile(dietitianId, request));
    }

    // ✅ 영양사 비밀번호 변경 (current_pw 검증 후 new_pw 저장)
    @PutMapping("/{dietitianId}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long dietitianId,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        dietitianService.changeDietitianPassword(dietitianId, request);
        return ResponseEntity.noContent().build();
    }

    // ✅ (옵션) 학교정보 수정/재기재
    @PutMapping("/{dietitianId}/school")
    public ResponseEntity<SchoolResponse> upsertSchool(@PathVariable Long dietitianId,
                                                       @Valid @RequestBody SchoolRequest request) {
        return ResponseEntity.ok(dietitianService.upsertSchool(dietitianId, request));
    }
}
