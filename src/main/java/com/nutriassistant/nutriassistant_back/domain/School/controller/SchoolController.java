package com.nutriassistant.nutriassistant_back.domain.School.controller;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.School.dto.NeisSchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.service.NeisSchoolService;
import com.nutriassistant.nutriassistant_back.domain.School.service.SchoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final NeisSchoolService neisSchoolService;
    private final SchoolService schoolService;
    private final DietitianRepository dietitianRepository; // [추가] 로그인한 영양사 ID 찾기용

    // =========================================================================
    // 1. 학교 검색 (기존 코드 유지)
    // =========================================================================
    @GetMapping("/search")
    public ResponseEntity<List<SchoolResponse>> searchSchool(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "school_kind", required = false) String schoolKind
    ) {
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String normalizedSchoolKind = normalizeSchoolKind(schoolKind);
        List<NeisSchoolResponse.SchoolRow> neisRows =
                neisSchoolService.searchSchools(keyword.trim(), normalizedSchoolKind);

        List<SchoolResponse> result = neisRows.stream()
                .map(row -> SchoolResponse.builder()
                        .schoolName(row.getSchoolName())
                        .regionCode(row.getRegionCode())
                        .schoolCode(row.getSchoolCode())
                        .address(row.getAddress())
                        .build())
                .toList();

        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // 2. 내 학교 등록 (POST) - 영양사 전용
    // =========================================================================
    @PostMapping("/register")
    public ResponseEntity<SchoolResponse> registerSchool(
            @RequestBody SchoolRequest request,
            Authentication authentication // Spring Security가 넣어주는 인증 객체
    ) {
        // 1. 현재 로그인한 영양사의 PK(ID)를 찾습니다.
        Long currentDietitianId = getDietitianId(authentication);

        // 2. 서비스 호출
        SchoolResponse response = schoolService.registerSchool(currentDietitianId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // 3. 내 학교 정보 조회 (GET) - 영양사 전용
    // =========================================================================
    @GetMapping("/my")
    public ResponseEntity<SchoolResponse> getMySchool(Authentication authentication) {
        Long currentDietitianId = getDietitianId(authentication);
        SchoolResponse response = schoolService.getMySchool(currentDietitianId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 4. 내 학교 정보 수정 (PUT) - 영양사 전용
    // =========================================================================
    @PutMapping("/my")
    public ResponseEntity<SchoolResponse> updateSchool(
            @RequestBody SchoolRequest request,
            Authentication authentication
    ) {
        Long currentDietitianId = getDietitianId(authentication);
        SchoolResponse response = schoolService.updateSchoolInfo(currentDietitianId, request);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Helper 메서드
    // =========================================================================

    /**
     * 인증 객체(Authentication)에서 username을 꺼내 DB에서 조회 후 ID 반환
     */
    private Long getDietitianId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String username = authentication.getName(); // 토큰에 담긴 username (Subject)

        Dietitian dietitian = dietitianRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 영양사 계정입니다."));

        return dietitian.getId();
    }

    private String normalizeSchoolKind(String schoolKind) {
        if (schoolKind == null) return null;
        String k = schoolKind.trim();
        if (k.isEmpty()) return null;
        return switch (k) {
            case "초", "초등", "초등학교" -> "초등학교";
            case "중", "중등", "중학교" -> "중학교";
            case "고", "고등", "고등학교" -> "고등학교";
            default -> k;
        };
    }
}