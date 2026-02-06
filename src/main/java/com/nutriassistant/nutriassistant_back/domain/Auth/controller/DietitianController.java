package com.nutriassistant.nutriassistant_back.domain.Auth.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.*;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.DietitianService;
import com.nutriassistant.nutriassistant_back.domain.Auth.service.AuthService; // [추가] 탈퇴 로직용
import com.nutriassistant.nutriassistant_back.global.jwt.CustomUserDetails; // [권장] CustomUserDetails 사용
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [추가] log 심볼 해결
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j // [추가] 심볼 'log' 해결
@RestController
@RequestMapping("/api/dietitian")
@RequiredArgsConstructor // [추가] 생성자 주입 자동화
public class DietitianController {

    private final DietitianService dietitianService;
    private final AuthService authService; // [추가] 탈퇴 로직 처리를 위한 주입

    /*
     * [영양사 회원가입 및 학교 정보 기재 API]
     */
    @PostMapping("/signup")
    public ResponseEntity<DietitianSignUpResponse> signup(@Valid @RequestBody DietitianSignUpRequest request) {
        log.info("📝 영양사 회원가입 요청: {}", request.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(dietitianService.signupWithSchool(request));
    }

    /*
     * [내 정보 조회 API]
     * URL: GET /api/dietitian/me
     */
    @GetMapping("/me")
    public ResponseEntity<DietitianProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        log.info("🔍 영양사 프로필 조회: {}", username);

        Long currentDietitianId = dietitianService.findIdByUsername(username);
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

        String username = userDetails.getUsername();
        log.info("✏️ 영양사 프로필 수정: {}", username);

        Long currentDietitianId = dietitianService.findIdByUsername(username);
        return ResponseEntity.ok(dietitianService.updateDietitianProfile(currentDietitianId, request));
    }

    /*
     * [내 비밀번호 변경 API]
     * URL: PUT /api/dietitian/me/password
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {

        String username = userDetails.getUsername();
        log.info("🔒 영양사 비밀번호 변경 요청: {}", username);

        Long currentDietitianId = dietitianService.findIdByUsername(username);
        authService.changeDietitianPassword(currentDietitianId, request);

        return ResponseEntity.noContent().build();
    }

    /*
     * [학교 정보 수정 API]
     * URL: PUT /api/dietitian/me/school
     */
    @PutMapping("/me/school")
    public ResponseEntity<SchoolResponse> upsertSchool(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SchoolRequest request) {

        String username = userDetails.getUsername();
        Long currentDietitianId = dietitianService.findIdByUsername(username);

        log.info("🏫 학교 정보 수정 요청: DietitianID={}", currentDietitianId);
        return ResponseEntity.ok(dietitianService.upsertSchool(currentDietitianId, request));
    }

    /*
     * [영양사 회원 탈퇴 API]
     * URL: POST /api/dietitian/me/withdraw
     * 보안을 위해 본인 확인용 비밀번호를 검증합니다.
     */
    @PostMapping("/me/withdraw")
    public ResponseEntity<Void> withdrawDietitian(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawalRequest request) {

        String username = userDetails.getUsername();
        log.info("🚪 영양사 회원 탈퇴 요청: {}", username);

        // 1. 토큰에서 유저네임 추출 후 ID 조회
        Long currentDietitianId = dietitianService.findIdByUsername(username);

        // 2. AuthService의 탈퇴 로직 실행 (비밀번호 검증, 학교 연동 해제, Soft Delete 포함)
        authService.withdrawDietitian(currentDietitianId, request.getPassword());

        return ResponseEntity.noContent().build();
    }
}