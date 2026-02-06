package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.DietitianService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/*
 * 영양사 관련 API 요청을 처리하는 컨트롤러 클래스
 */
@RestController
@RequestMapping("/api/dietitian")
public class DietitianController {

    private final DietitianService dietitianService;

    public DietitianController(DietitianService dietitianService) {
        this.dietitianService = dietitianService;
    }

    /*
     * [영양사 회원가입 및 학교 정보 기재 API]
     */
    @PostMapping("/signup")
    public ResponseEntity<DietitianSignUpResponse> signup(@Valid @RequestBody DietitianSignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dietitianService.signupWithSchool(request));
    }

    /*
     * [내 정보 조회 API]
     * URL: GET /api/dietitian/me
     */
    @GetMapping("/me")
    public ResponseEntity<DietitianProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        Long currentDietitianId = dietitianService.findIdByEmail(email);

        return ResponseEntity.ok(dietitianService.getDietitianProfile(currentDietitianId));
    }

    /*
     * [내 정보 수정 API]
     * URL: PATCH /api/dietitian/me
     */
    @PatchMapping("/me")
    public ResponseEntity<DietitianProfileResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DietitianUpdateRequest request) {

        String email = userDetails.getUsername();
        Long currentDietitianId = dietitianService.findIdByEmail(email);

        return ResponseEntity.ok(dietitianService.updateDietitianProfile(currentDietitianId, request));
    }

    /*
     * [내 비밀번호 변경 API] - (수정됨: 안전한 토큰 방식)
     * URL: PUT /api/dietitian/me/password
     * 토큰에서 ID를 추출하므로 URL에 ID를 노출하지 않습니다.
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {

        // 1. 토큰에서 이메일 추출 -> ID 찾기
        String email = userDetails.getUsername();
        Long currentDietitianId = dietitianService.findIdByEmail(email);

        // 2. 서비스 호출
        dietitianService.changeDietitianPassword(currentDietitianId, request);

        return ResponseEntity.noContent().build();
    }

    // ⚠️ 삭제됨: 중복된 옛날 changePassword 메서드는 지웠습니다.

    /*
     * [학교 정보 수정 API]
     * URL: PUT /api/dietitian/{dietitianId}/school
     * (참고: 이 부분도 나중에 /me/school 로 바꾸시면 더 안전합니다. 일단은 유지했습니다.)
     */
    @PutMapping("/{dietitianId}/school")
    public ResponseEntity<SchoolResponse> upsertSchool(@PathVariable Long dietitianId,
                                                       @Valid @RequestBody SchoolRequest request) {
        return ResponseEntity.ok(dietitianService.upsertSchool(dietitianId, request));
    }
}