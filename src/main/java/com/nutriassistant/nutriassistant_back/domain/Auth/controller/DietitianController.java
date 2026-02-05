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
     * 토큰(UserDetails)에서 이메일을 추출하여 현재 로그인한 영양사의 프로필을 조회합니다.
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
     * 토큰 기반으로 본인의 정보를 수정합니다.
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
     * [관리자용 프로필 수정]
     * URL 경로에 명시된 ID의 영양사 정보를 수정합니다.
     */
    @PutMapping("/{dietitianId}")
    public ResponseEntity<DietitianProfileResponse> updateProfile(@PathVariable Long dietitianId,
                                                                  @Valid @RequestBody DietitianUpdateRequest request) {
        return ResponseEntity.ok(dietitianService.updateDietitianProfile(dietitianId, request));
    }

    /*
     * [비밀번호 변경 API]
     */
    @PutMapping("/{dietitianId}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long dietitianId,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        dietitianService.changeDietitianPassword(dietitianId, request);
        return ResponseEntity.noContent().build();
    }

    /*
     * [학교 정보 수정 API]
     */
    @PutMapping("/{dietitianId}/school")
    public ResponseEntity<SchoolResponse> upsertSchool(@PathVariable Long dietitianId,
                                                       @Valid @RequestBody SchoolRequest request) {
        return ResponseEntity.ok(dietitianService.upsertSchool(dietitianId, request));
    }
}