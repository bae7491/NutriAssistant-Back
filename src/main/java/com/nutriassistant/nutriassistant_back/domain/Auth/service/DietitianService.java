package com.nutriassistant.nutriassistant_back.domain.Auth.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.RefreshTokenRepository;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.domain.School.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DietitianService {

    private final DietitianRepository dietitianRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository; // 리프레시 토큰 삭제용

    /**
     * ✅ [회원 탈퇴] 영양사 탈퇴 (Soft Delete)
     * - 학교 데이터는 남겨두되, 담당자 연결만 해제합니다.
     */
    @Transactional
    public void withdrawDietitian(Long dietitianId) {
        // 1. 영양사 조회
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        // 2. 담당 학교와의 연결 해제 (학교 데이터 자체는 유지)
        schoolRepository.findByDietitian_Id(dietitianId).ifPresent(school -> {
            school.setDietitian(null); // 연관관계 해제
            schoolRepository.save(school);
            log.info("학교 [{}]에서 영양사 연결이 해제되었습니다.", school.getSchoolName());
        });

        // 3. 영양사 상태 변경 (Soft Delete)
        dietitian.withdraw();
        dietitianRepository.save(dietitian);

        // 4. 보안 조치: 리프레시 토큰 삭제 (로그인 원천 차단)
        refreshTokenRepository.deleteByUsername(dietitian.getUsername());

        log.info("영양사 계정 탈퇴 처리 완료: ID={}", dietitianId);
    }

    /**
     * ✅ [회원가입] 영양사 정보 생성 + 학교 정보 등록
     */
    @Transactional
    public DietitianSignUpResponse signupWithSchool(DietitianSignUpRequest request) {

        // 1. 유효성 및 중복 검사
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디(username)는 필수입니다.");
        }
        if (dietitianRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
        }

        // 2. 영양사 엔티티 생성
        Dietitian dietitian = new Dietitian();
        dietitian.setUsername(username);
        dietitian.setName(request.getName());
        dietitian.setPhone(request.getPhone());
        dietitian.setPasswordHash(passwordEncoder.encode(request.getPw()));

        Dietitian savedDietitian = dietitianRepository.save(dietitian);

        // 3. 학교 엔티티 생성 (Builder 패턴 사용)
        SchoolRequest sr = request.getSchool();
        School savedSchool = null;

        if (sr != null) {
            School school = School.builder()
                    .dietitian(savedDietitian)
                    .schoolName(sr.getSchoolName())
                    .regionCode(sr.getRegionCode())
                    .schoolCode(sr.getSchoolCode())
                    .address(sr.getAddress())
                    .schoolType(sr.getSchoolType())
                    .phone(sr.getPhone())
                    .email(sr.getEmail())
                    .studentCount(sr.getStudentCount())
                    .targetUnitPrice(sr.getTargetUnitPrice())
                    .maxUnitPrice(sr.getMaxUnitPrice())
                    .operationRules(sr.getOperationRules())
                    .cookWorkers(sr.getCookWorkers())
                    .kitchenEquipment(sr.getKitchenEquipment())
                    .build();

            savedSchool = schoolRepository.save(school);
        }

        return new DietitianSignUpResponse(savedDietitian, savedSchool);
    }

    /**
     * ✅ [프로필 수정] 영양사 개인정보 수정
     */
    @Transactional
    public DietitianProfileResponse updateDietitianProfile(Long dietitianId, DietitianUpdateRequest req) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        dietitian.setName(req.getName());
        dietitian.setPhone(req.getPhone());

        Dietitian saved = dietitianRepository.save(dietitian);
        School school = schoolRepository.findByDietitian_Id(dietitianId).orElse(null);

        return new DietitianProfileResponse(saved, school);
    }

    /**
     * ✅ [비밀번호 변경]
     */
    @Transactional
    public void changeDietitianPassword(Long dietitianId, PasswordChangeRequest req) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(req.getCurrentPassword(), dietitian.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }

        dietitian.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
    }

    /**
     * ✅ [학교 정보 등록/수정] (Upsert)
     */
    @Transactional
    public SchoolResponse upsertSchool(Long dietitianId, SchoolRequest req) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        Optional<School> existingSchoolOp = schoolRepository.findByDietitian_Id(dietitianId);

        School school;
        if (existingSchoolOp.isPresent()) {
            school = existingSchoolOp.get();

            school.setPhone(req.getPhone());
            school.setEmail(req.getEmail());
            school.setStudentCount(req.getStudentCount());
            school.setTargetUnitPrice(req.getTargetUnitPrice());
            school.setMaxUnitPrice(req.getMaxUnitPrice());
            school.setOperationRules(req.getOperationRules());
            school.setCookWorkers(req.getCookWorkers());
            school.setKitchenEquipment(req.getKitchenEquipment());

            log.info("기존 학교 정보 업데이트 완료: {}", school.getSchoolName());

        } else {
            school = School.builder()
                    .dietitian(dietitian)
                    .schoolName(req.getSchoolName())
                    .schoolCode(req.getSchoolCode())
                    .regionCode(req.getRegionCode())
                    .address(req.getAddress())
                    .schoolType(req.getSchoolType())
                    .phone(req.getPhone())
                    .email(req.getEmail())
                    .studentCount(req.getStudentCount())
                    .targetUnitPrice(req.getTargetUnitPrice())
                    .maxUnitPrice(req.getMaxUnitPrice())
                    .operationRules(req.getOperationRules())
                    .cookWorkers(req.getCookWorkers())
                    .kitchenEquipment(req.getKitchenEquipment())
                    .build();

            log.info("새로운 학교 등록 완료: {}", school.getSchoolName());
        }

        School saved = schoolRepository.save(school);
        return new SchoolResponse(saved);
    }
}