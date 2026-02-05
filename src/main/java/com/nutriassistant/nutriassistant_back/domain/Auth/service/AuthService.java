package com.nutriassistant.nutriassistant_back.domain.Auth.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.*;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.StudentRepository;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.domain.School.repository.SchoolRepository;
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
 * 통합 기능: 학생/영양사 회원가입, 로그인, 아이디/비번 찾기, 정보 수정
 */
@Service
public class AuthService {

    private final StudentRepository studentRepository;
    private final DietitianRepository dietitianRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(StudentRepository studentRepository,
                       DietitianRepository dietitianRepository,
                       SchoolRepository schoolRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.studentRepository = studentRepository;
        this.dietitianRepository = dietitianRepository;
        this.schoolRepository = schoolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    // =========================================================================
    // 1. 학생 회원가입
    // =========================================================================
    @Transactional
    public SignUpResponse signupStudent(SignUpRequest request) {
        Long requestSchoolId = request.getSchoolId();
        String username = request.getUsername() == null ? "" : request.getUsername().trim();

        // 1. 아이디 공백 체크
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디는 공백일 수 없습니다.");
        }

        // 2. 학교 존재 여부 확인
        School school = schoolRepository.findById(requestSchoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 학교 ID입니다."));

        // 3. 중복 아이디 체크 (해당 학교 내에서 유일해야 함)
        if (studentRepository.existsBySchoolAndUsername(school, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 학교에 이미 존재하는 아이디입니다.");
        }

        // 4. 엔티티 생성 및 데이터 세팅
        Student student = new Student();
        student.setSchool(school);
        student.setUsername(username);
        student.setName(request.getName());
        student.setPhone(request.getPhone());
        student.setGrade(request.getGrade());
        student.setClassNo(request.getClassNo());

        // 알레르기 리스트 -> CSV 변환
        student.setAllergyCodes(toAllergyCsv(request.getAllergyCodes()));

        // 5. 비밀번호 암호화 및 저장
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

        // 1. 영양사 아이디 중복 확인
        if (dietitianRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 영양사 아이디입니다.");
        }

        // 2. 영양사 엔티티 생성 및 저장
        Dietitian dietitian = new Dietitian();
        dietitian.setUsername(username);
        dietitian.setPasswordHash(passwordEncoder.encode(request.getPw()));
        dietitian.setName(request.getName());
        dietitian.setEmail(request.getEmail());
        dietitian.setPhone(request.getPhone());

        Dietitian savedDietitian = dietitianRepository.save(dietitian);

        // 3. 학교 정보 처리
        SchoolRequest schoolReq = request.getSchoolInfo();

        // 3-1. 학교 중복 가입 방지
        if (schoolRepository.findBySchoolCode(schoolReq.getSchoolCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 시스템에 등록된 학교입니다.");
        }

        // 3-2. 학교 엔티티 빌드 (영양사와 연결)
        School school = School.builder()
                .dietitian(savedDietitian)
                .schoolName(schoolReq.getSchoolName())
                .regionCode(schoolReq.getRegionCode())
                .schoolCode(schoolReq.getSchoolCode())
                .address(schoolReq.getAddress())
                .schoolType(schoolReq.getSchoolType())
                .build();

        School savedSchool = schoolRepository.save(school);

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
    // 5. 아이디 찾기 (이름 + 전화번호)
    // =========================================================================
    @Transactional(readOnly = true)
    public String findUsername(FindIdRequest request) {
        // 전화번호 정규화 (숫자만 남김)
        String purePhone = request.getPhone().replaceAll("[^0-9]", "");

        // 1. 학생 테이블 조회
        var studentOpt = studentRepository.findByNameAndPhone(request.getName(), purePhone);
        if (studentOpt.isPresent()) {
            return studentOpt.get().getUsername();
        }

        // 2. 영양사 테이블 조회
        var dietitianOpt = dietitianRepository.findByNameAndPhone(request.getName(), purePhone);
        if (dietitianOpt.isPresent()) {
            return dietitianOpt.get().getUsername();
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "입력하신 정보와 일치하는 계정이 없습니다.");
    }

    // =========================================================================
    // 6. 비밀번호 찾기 (아이디 + 이름 + 전화번호 -> 임시 비번 발급)
    // =========================================================================
    @Transactional
    public String resetPassword(FindPasswordRequest request) {
        String purePhone = request.getPhone().replaceAll("[^0-9]", "");

        // 임시 비밀번호 생성 (UUID 앞 8자리)
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        String encodedPassword = passwordEncoder.encode(tempPassword);

        // 1. 학생 확인 및 변경
        var studentOpt = studentRepository.findByUsernameAndNameAndPhone(
                request.getUsername(), request.getName(), purePhone);
        if (studentOpt.isPresent()) {
            studentOpt.get().setPasswordHash(encodedPassword);
            return tempPassword;
        }

        // 2. 영양사 확인 및 변경
        var dietitianOpt = dietitianRepository.findByUsernameAndNameAndPhone(
                request.getUsername(), request.getName(), purePhone);
        if (dietitianOpt.isPresent()) {
            dietitianOpt.get().setPasswordHash(encodedPassword);
            return tempPassword;
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "입력하신 정보와 일치하는 계정이 없습니다.");
    }

    // =========================================================================
    // 7. 학생 정보 수정
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
    // 8. 학생 비밀번호 변경 (로그인 상태에서 변경)
    // =========================================================================
    @Transactional
    public void changeStudentPassword(Long studentId, PasswordChangeRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생 정보를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 기존과 같은지 확인
        if (passwordEncoder.matches(request.getNewPassword(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새로운 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        // 변경
        student.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    }

    // =========================================================================
    // 유틸리티: 알레르기 리스트 -> CSV 변환
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