package com.nutriassistant.nutriassistant_back.domain.School.controller;

import com.nutriassistant.nutriassistant_back.domain.School.dto.NeisSchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolSearchDto;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolUpdateRequest; // ✅ 수정용 DTO 임포트
import com.nutriassistant.nutriassistant_back.domain.School.service.SchoolService;
import com.nutriassistant.nutriassistant_back.global.jwt.CustomUserDetails;

import jakarta.validation.Valid; // 유효성 검사
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    // 1. [학생용] 학교 검색
    @GetMapping("/search")
    public ResponseEntity<List<SchoolSearchDto>> searchSchoolsForUser(@RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(schoolService.searchSchoolsForUser(keyword));
    }

    // 2. [영양사용] 학교 검색
    @GetMapping("/dietitian/search")
    public ResponseEntity<List<NeisSchoolResponse.SchoolRow>> searchSchoolsForDietitian(@RequestParam("keyword") String keyword) {
        return ResponseEntity.ok(schoolService.searchSchools(keyword));
    }

    // 3. 내 학교 등록 (POST)
    @PostMapping("/register")
    public ResponseEntity<SchoolResponse> registerSchool(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SchoolRequest request
    ) {
        Long dietitianId = userDetails.getId();
        SchoolResponse response = schoolService.registerSchool(dietitianId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 4. 내 학교 정보 조회 (GET)
    @GetMapping("/my")
    public ResponseEntity<SchoolResponse> getMySchool(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long dietitianId = userDetails.getId();
        SchoolResponse response = schoolService.getMySchool(dietitianId);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 5. 내 학교 정보 수정 (PATCH 권장)
    // =========================================================================
    @PatchMapping("/my") // 👈 [핵심 수정] PutMapping -> PatchMapping 으로 변경
    public ResponseEntity<SchoolResponse> updateSchool(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid SchoolUpdateRequest request
    ) {
        Long dietitianId = userDetails.getId();
        SchoolResponse response = schoolService.updateSchoolInfo(dietitianId, request);
        return ResponseEntity.ok(response);
    }
}