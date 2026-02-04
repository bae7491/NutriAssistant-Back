package com.nutriassistant.nutriassistant_back.domain.Auth.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
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
            // ★ 수정됨: new School() 대신 builder() 사용
            // Entity 정의상 schoolName, schoolCode 등은 Setter가 없으므로 생성 시점에 넣어야 합니다.
            School school = School.builder()
                    .dietitian(savedDietitian) // 연관관계 설정
                    .schoolName(sr.getSchoolName())
                    .regionCode(sr.getRegionCode())
                    .schoolCode(sr.getSchoolCode())
                    .address(sr.getAddress())
                    .schoolType(sr.getSchoolType())
                    // 선택적 필드 (운영 정보) - Builder에 포함하거나, 생성 후 Setter 사용 가능
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

        // 명시적 save (선택사항, Dirty Checking 작동함)
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
     * - 기존 학교가 있으면 -> 운영 정보만 업데이트 (학교 코드 등 식별자는 변경 불가)
     * - 기존 학교가 없으면 -> 새로 생성
     */
    @Transactional
    public SchoolResponse upsertSchool(Long dietitianId, SchoolRequest req) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        Optional<School> existingSchoolOp = schoolRepository.findByDietitian_Id(dietitianId);

        School school;
        if (existingSchoolOp.isPresent()) {
            // [CASE 1] 이미 학교가 등록된 경우 -> 운영 정보만 수정
            // (주의: Entity 설계상 SchoolName, SchoolCode, RegionCode는 수정 불가능하도록 Setter를 막았습니다.)
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
            // [CASE 2] 학교가 없는 경우 -> 새로 생성 (Builder 사용)
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