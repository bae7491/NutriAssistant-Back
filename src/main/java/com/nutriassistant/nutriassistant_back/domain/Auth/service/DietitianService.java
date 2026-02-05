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
    private final RefreshTokenRepository refreshTokenRepository;

    /*
     * [ID 조회] Username(Email)으로 회원 ID 조회
     * 인증 객체에서 추출한 username을 통해 DB의 PK(ID)를 찾습니다.
     * 조회 전용 트랜잭션으로 성능을 최적화합니다.
     */
    @Transactional(readOnly = true)
    public Long findIdByUsername(String username) {
        return dietitianRepository.findByUsername(username)
                .map(Dietitian::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    /*
     * [영양사 프로필 조회]
     * 영양사 ID를 기반으로 본인의 정보와 소속된 학교 정보를 조회합니다.
     * 학교 정보가 없을 경우 School 객체는 null로 처리되어 응답에 포함됩니다.
     */
    @Transactional(readOnly = true)
    public DietitianProfileResponse getDietitianProfile(Long dietitianId) {
        // 1. 영양사 엔티티 조회
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        // 2. 해당 영양사가 담당하는 학교 정보 조회 (없을 수도 있음)
        School school = schoolRepository.findByDietitian_Id(dietitianId).orElse(null);

        // 3. DTO 변환 후 반환
        return new DietitianProfileResponse(dietitian, school);
    }

    /*
     * [회원 탈퇴] 영양사 탈퇴 (Soft Delete)
     * 학교 데이터는 유지하되 영양사 연결만 해제합니다.
     * 리프레시 토큰을 삭제하여 재로그인을 방지합니다.
     */
    @Transactional
    public void withdrawDietitian(Long dietitianId) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        schoolRepository.findByDietitian_Id(dietitianId).ifPresent(school -> {
            school.setDietitian(null);
            schoolRepository.save(school);
            log.info("학교 [{}]에서 영양사 연결이 해제되었습니다.", school.getSchoolName());
        });

        dietitian.withdraw();
        dietitianRepository.save(dietitian);
        refreshTokenRepository.deleteByUsername(dietitian.getUsername());
        log.info("영양사 계정 탈퇴 처리 완료: ID={}", dietitianId);
    }

    /*
     * [회원가입] 영양사 생성 및 기존 학교 연결 또는 새 학교 생성
     * 유효성 검사 후 영양사를 저장하고, 요청된 학교 정보에 따라
     * 기존 학교에 연결하거나 새로운 학교를 생성합니다.
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

        // 2. 영양사 엔티티 생성 및 저장
        Dietitian dietitian = new Dietitian();
        dietitian.setUsername(username);
        dietitian.setName(request.getName());
        dietitian.setPhone(request.getPhone());
        dietitian.setPasswordHash(passwordEncoder.encode(request.getPw()));

        Dietitian savedDietitian = dietitianRepository.save(dietitian);

        // 3. 학교 정보 처리 (기존 학교 연결 로직 추가)
        SchoolRequest sr = request.getSchool();
        School savedSchool = null;

        if (sr != null) {
            // 3-1. 학교 코드로 기존 학교가 있는지 조회
            Optional<School> existingSchoolOp = schoolRepository.findBySchoolCode(sr.getSchoolCode());

            if (existingSchoolOp.isPresent()) {
                // [CASE A] 이미 학교가 존재하는 경우
                School existingSchool = existingSchoolOp.get();

                // 이미 다른 영양사가 담당하고 있는지 확인
                if (existingSchool.getDietitian() != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 학교에는 이미 담당 영양사가 존재합니다.");
                }

                // 기존 학교에 새 영양사 연결
                existingSchool.setDietitian(savedDietitian);

                // 가입 시 입력한 최신 정보로 학교 정보 갱신
                existingSchool.setSchoolName(sr.getSchoolName());
                existingSchool.setAddress(sr.getAddress());
                existingSchool.setPhone(sr.getPhone());
                existingSchool.setEmail(sr.getEmail());
                existingSchool.setStudentCount(sr.getStudentCount());
                existingSchool.setTargetUnitPrice(sr.getTargetUnitPrice());
                existingSchool.setMaxUnitPrice(sr.getMaxUnitPrice());
                existingSchool.setOperationRules(sr.getOperationRules());
                existingSchool.setCookWorkers(sr.getCookWorkers());
                existingSchool.setKitchenEquipment(sr.getKitchenEquipment());

                savedSchool = schoolRepository.save(existingSchool);
                log.info("기존 학교({})에 새로운 영양사를 연결했습니다.", savedSchool.getSchoolName());

            } else {
                // [CASE B] 학교가 존재하지 않는 경우 (신규 생성)
                School newSchool = School.builder()
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

                savedSchool = schoolRepository.save(newSchool);
                log.info("새로운 학교({})를 생성하고 영양사를 연결했습니다.", savedSchool.getSchoolName());
            }
        }

        return new DietitianSignUpResponse(savedDietitian, savedSchool);
    }

    /*
     * [프로필 수정]
     * 영양사의 이름, 전화번호 등 개인 정보를 수정합니다.
     */
    @Transactional
    public DietitianProfileResponse updateDietitianProfile(Long dietitianId, DietitianUpdateRequest req) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        if (req.getName() != null && !req.getName().isBlank()) {
            dietitian.setName(req.getName());
        }
        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            dietitian.setPhone(req.getPhone());
        }

        Dietitian saved = dietitianRepository.save(dietitian);
        School school = schoolRepository.findByDietitian_Id(dietitianId).orElse(null);
        return new DietitianProfileResponse(saved, school);
    }

    /*
     * [비밀번호 변경]
     * 현재 비밀번호 확인 후 새로운 비밀번호로 변경합니다.
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

    /*
     * [학교 정보 등록/수정] (Upsert)
     * 영양사에 연결된 학교 정보를 수정하거나, 없으면 새로 등록합니다.
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
        }

        School saved = schoolRepository.save(school);
        return new SchoolResponse(saved);
    }

    /*
     * [ID 조회] 이메일로 ID 조회 (Controller 연동용)
     */
    @Transactional(readOnly = true)
    public Long findIdByEmail(String email) {
        return findIdByUsername(email);
    }
}