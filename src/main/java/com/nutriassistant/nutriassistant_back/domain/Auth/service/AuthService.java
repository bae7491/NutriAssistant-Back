package com.nutriassistant.nutriassistant_back.domain.Auth.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.*;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.RefreshTokenRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.StudentRepository;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.domain.School.repository.SchoolRepository;
import com.nutriassistant.nutriassistant_back.global.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nutriassistant.nutriassistant_back.global.jwt.JwtProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * [인증(Authentication) 관련 비즈니스 로직 처리 서비스]
 * 통합 기능: 학생/영양사 회원가입, 로그인, 아이디/비번 찾기, 정보 수정, 비번 변경, 회원 탈퇴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StudentRepository studentRepository;
    private final DietitianRepository dietitianRepository;
    private final SchoolRepository schoolRepository;
    private final RefreshTokenRepository refreshTokenRepository; // 리프레시 토큰 삭제용
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // =========================================================================
    // 1. 학생 회원가입
    // =========================================================================
    @Transactional
    public SignUpResponse signupStudent(SignUpRequest request) {
        Long requestSchoolId = request.getSchoolId();
        String username = request.getUsername() == null ? "" : request.getUsername().trim();

        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디는 공백일 수 없습니다.");
        }

        School school = schoolRepository.findById(requestSchoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 학교 ID입니다."));

        if (studentRepository.existsBySchoolAndUsername(school, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 학교에 이미 존재하는 아이디입니다.");
        }
        // 2. 전화번호 중복 체크 [추가]
        String purePhone = request.getPhone().replaceAll("[^0-9]", "");
        if (studentRepository.existsByPhone(purePhone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 전화번호입니다.");
        }

        Student student = new Student();
        student.setSchool(school);
        student.setUsername(username);
        student.setName(request.getName());
        student.setPhone(request.getPhone());
        student.setGrade(request.getGrade());
        student.setClassNo(request.getClassNo());
        student.setAllergyCodes(toAllergyCsv(request.getAllergyCodes()));
        student.setPasswordHash(passwordEncoder.encode(request.getPw()));

        Student saved = studentRepository.save(student);

        return new SignUpResponse(saved);
    }

    // =========================================================================
    // 2. 영양사 회원가입 (학교 정보 동시 등록)
    // =========================================================================
    @Transactional
    public DietitianSignUpResponse signupDietitian(DietitianSignUpRequest request) {
        String username = request.getUsername().trim();

        if (dietitianRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 영양사 아이디입니다.");
        }
        // 2. 이메일 중복 체크 [추가]
        if (dietitianRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        // 3. 전화번호 중복 체크 [추가]
        String purePhone = request.getPhone().replaceAll("[^0-9]", "");
        if (dietitianRepository.existsByPhone(purePhone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 전화번호입니다.");
        }

        Dietitian dietitian = new Dietitian();
        dietitian.setUsername(username);
        dietitian.setPasswordHash(passwordEncoder.encode(request.getPw()));
        dietitian.setName(request.getName());
        dietitian.setEmail(request.getEmail()); // 이메일 저장
        dietitian.setPhone(request.getPhone());

        Dietitian savedDietitian = dietitianRepository.save(dietitian);

        SchoolRequest schoolReq = request.getSchoolInfo();
        School savedSchool = null;

        if (schoolReq != null) {
            // 학교 코드로 기존 학교가 있는지 조회 (중복 가입 방지 or 연결)
            if (schoolRepository.findBySchoolCode(schoolReq.getSchoolCode()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 시스템에 등록된 학교입니다.");
            }

            School school = School.builder()
                    .dietitian(savedDietitian)
                    .schoolName(schoolReq.getSchoolName())
                    .regionCode(schoolReq.getRegionCode())
                    .schoolCode(schoolReq.getSchoolCode())
                    .address(schoolReq.getAddress())
                    .schoolType(schoolReq.getSchoolType())
                    .phone(schoolReq.getPhone())
                    .email(schoolReq.getEmail())
                    .studentCount(schoolReq.getStudentCount())
                    .targetUnitPrice(schoolReq.getTargetUnitPrice())
                    .maxUnitPrice(schoolReq.getMaxUnitPrice())
                    .operationRules(schoolReq.getOperationRules())
                    .cookWorkers(schoolReq.getCookWorkers())
                    .kitchenEquipment(schoolReq.getKitchenEquipment())
                    .build();

            savedSchool = schoolRepository.save(school);
        }

        return new DietitianSignUpResponse(savedDietitian, savedSchool);
    }

    // =========================================================================
    // 3. 학생 로그인
    // =========================================================================
    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        Student student = studentRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(request.getPw(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }

        // [개선] 상태값과 날짜 중 하나라도 탈퇴 징후가 있으면 차단
        if (student.getStatus() != UserStatus.ACTIVE || student.getWithdrawalDate() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "탈퇴하거나 이용이 제한된 계정입니다. 복구는 관리자에게 문의하세요.");
        }

        return jwtProvider.createToken(
                student.getId(),
                student.getUsername(),
                student.getSchoolId(),
                "ROLE_STUDENT"
        );
    }

    // =========================================================================
    // 4. 영양사 로그인
    // =========================================================================
    @Transactional(readOnly = true)
    public String loginDietitian(LoginRequest request) {
        Dietitian dietitian = dietitianRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 영양사 아이디입니다."));

        if (!passwordEncoder.matches(request.getPw(), dietitian.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }

        // [개선] 상태값과 날짜 중 하나라도 탈퇴 징후가 있으면 차단
        if (dietitian.getStatus() != UserStatus.ACTIVE || dietitian.getWithdrawalDate() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "탈퇴하거나 이용이 제한된 계정입니다. 복구는 관리자에게 문의하세요.");
        }

        School school = schoolRepository.findByDietitian_Id(dietitian.getId()).orElse(null);
        Long schoolId = (school != null) ? school.getId() : null;

        return jwtProvider.createToken(
                dietitian.getId(),
                dietitian.getUsername(),
                schoolId,
                "ROLE_DIETITIAN"
        );
    }

    // =========================================================================
    // 5. 아이디 찾기 (학생: 이름+전화번호 / 영양사: 이름+이메일)
    // =========================================================================

    // [학생]
    @Transactional(readOnly = true)
    public String findStudentId(StudentFindIdRequest request) {
        String purePhone = request.getPhone().replaceAll("[^0-9]", "");
        return studentRepository.findByNameAndPhone(request.getName(), purePhone)
                .map(Student::getUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일치하는 학생 정보가 없습니다."));
    }

    // [영양사]
    @Transactional(readOnly = true)
    public String findDietitianId(DietitianFindIdRequest request) {
        return dietitianRepository.findByNameAndEmail(request.getName(), request.getEmail())
                .map(Dietitian::getUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일치하는 영양사 정보가 없습니다."));
    }

    // =========================================================================
    // 6. 비밀번호 찾기 (학생: 임시발급 / 영양사: 임시발급 리턴)
    // =========================================================================

    // [학생] 아이디 + 이름 + 전화번호 일치 시 임시 비밀번호 발급
    @Transactional
    public String findStudentPassword(StudentFindPasswordRequest request) {
        String purePhone = request.getPhone().replaceAll("[^0-9]", "");

        // 주의: getEmail()로 되어 있지만 DTO에 따라 getUsername()일 수 있음. 확인 필요.
        Student student = studentRepository.findByUsernameAndNameAndPhone(
                        request.getEmail(), request.getName(), purePhone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일치하는 학생 계정이 없습니다."));

        // 임시 비밀번호 생성 (8자리)
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        student.setPasswordHash(passwordEncoder.encode(tempPassword));

        return tempPassword;
    }

    // [영양사] 아이디 + 이름 + 이메일 일치 시 임시 비밀번호 발급
    @Transactional
    public String findDietitianPassword(DietitianFindPasswordRequest request) {
        Dietitian dietitian = dietitianRepository.findByUsernameAndNameAndEmail(
                        request.getUsername(), request.getName(), request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일치하는 영양사 계정이 없습니다."));

        // 임시 비밀번호 생성
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        dietitian.setPasswordHash(passwordEncoder.encode(tempPassword));

        // [수정] 테스트를 위해 임시 비밀번호를 리턴합니다. (나중에 이메일 발송으로 변경 가능)
        return tempPassword;
    }

    // =========================================================================
    // 7. 정보 수정 (학생)
    // =========================================================================
    @Transactional
    public SignUpResponse updateStudentProfile(Long studentId, StudentUpdateRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생 정보를 찾을 수 없습니다."));

        if (request.getName() != null) student.setName(request.getName());
        if (request.getPhone() != null) student.setPhone(request.getPhone());
        if (request.getGrade() != null) student.setGrade(request.getGrade());
        if (request.getClassNo() != null) student.setClassNo(request.getClassNo());

        if (request.getAllergyCodes() != null) {
            student.setAllergyCodes(toAllergyCsv(request.getAllergyCodes()));
        }

        return new SignUpResponse(student);
    }

    // =========================================================================
    // 8. 비밀번호 변경 (로그인 상태) - 학생 / 영양사
    // =========================================================================

    // [학생]
    @Transactional
    public void changeStudentPassword(Long studentId, PasswordChangeRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새로운 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }
        student.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    }

    // [영양사]
    @Transactional
    public void changeDietitianPassword(Long dietitianId, PasswordChangeRequest request) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), dietitian.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(request.getNewPassword(), dietitian.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새로운 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }
        dietitian.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    }

    // =========================================================================
// 9. 회원 탈퇴 (학생 / 영양사)
// =========================================================================

    /**
     * [학생 회원 탈퇴]
     * 상태 변경(WITHDRAWN) 및 탈퇴 날짜 기록, 리프레시 토큰 삭제를 수행합니다.
     */
    @Transactional
    public void withdrawStudent(Long studentId, String inputPw) {
        // 1. 학생 정보 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        // 2. 비밀번호 일치 확인
        if (!passwordEncoder.matches(inputPw, student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않아 탈퇴할 수 없습니다.");
        }

        // 3. 리프레시 토큰 삭제 (중복 로그인 방지 및 세션 만료)
        // 리포지토리 메서드 명칭이 deleteByUsername인지 확인 필요 (보통 deleteByUsername 또는 deleteById)
        refreshTokenRepository.deleteByUsername(student.getUsername());

        // 4. Soft Delete 실행 (상태변경 + withdrawalDate 기록)
        student.withdraw();

        log.info("✅ 학생 회원 탈퇴 완료: ID={}, Username={}", studentId, student.getUsername());
    }

    /**
     * [영양사 회원 탈퇴]
     * 학교 연동 해제, 상태 변경 및 탈퇴 날짜 기록, 리프레시 토큰 삭제를 수행합니다.
     */
    @Transactional
    public void withdrawDietitian(Long dietitianId, String inputPw) {
        // 1. 영양사 정보 조회
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

        // 2. 비밀번호 일치 확인
        if (!passwordEncoder.matches(inputPw, dietitian.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않아 탈퇴할 수 없습니다.");
        }

        // 3. 학교 연동 해제
        // DB의 school 테이블 dietitian_id 컬럼에 NULL이 허용되어 있어야 합니다.
        schoolRepository.findByDietitian_Id(dietitianId).ifPresent(school -> {
            school.setDietitian(null);
            log.info("🏫 학교 연동 해제 완료: SchoolCode={}", school.getSchoolCode());
        });

        // 4. 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUsername(dietitian.getUsername());

        // 5. Soft Delete 실행 (상태변경 + withdrawalDate 기록)
        dietitian.withdraw();

        log.info("✅ 영양사 회원 탈퇴 완료: ID={}, Username={}", dietitianId, dietitian.getUsername());
    }

    // =========================================================================
    // 10. 이메일 중복체크
    // =========================================================================

    @Transactional(readOnly = true)
    public boolean checkStudentEmailAvailable(String email) {
        return studentRepository.findByUsername(email).isEmpty();
    }

    @Transactional(readOnly = true)
    public boolean checkDietitianEmailAvailable(String email) {
        return !dietitianRepository.existsByEmail(email);
    }

    // =========================================================================
    // 유틸리티
    // =========================================================================
    private String toAllergyCsv(List<Integer> codes) {
        if (codes == null || codes.isEmpty()) return "";
        return codes.stream()
                .filter(Objects::nonNull)
                .filter(c -> c >= 1 && c <= 19)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}