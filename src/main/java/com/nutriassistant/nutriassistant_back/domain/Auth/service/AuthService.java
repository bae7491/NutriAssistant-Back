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
import java.util.stream.Collectors;

/**
 * [인증(Authentication) 관련 비즈니스 로직 처리 서비스]
 *
 * 주요 기능:
 * 1. 학생: 회원가입, 로그인(JWT), 정보 수정, 비밀번호 변경
 * 2. 영양사: 회원가입(학교 정보 동시 등록), 로그인(JWT)
 *
 * 특징:
 * - JWT 토큰 발급 시 사용자 역할(Role)과 학교 ID(SchoolId)를 포함하여
 * 프론트엔드에서 권한 분리와 데이터 조회를 용이하게 합니다.
 * - 영양사 가입 시 '학교 등록'이 강제되는 비즈니스 로직을 수행합니다.
 */
@Service
public class AuthService {

    private final StudentRepository studentRepository;
    private final DietitianRepository dietitianRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 생성자 주입 (Constructor Injection)
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
    /**
     * [학생 회원가입]
     *
     * @param request 학생 가입 요청 정보 (학교ID, 아이디, 비번, 알레르기 등)
     * @return 가입된 학생 정보 (SignUpResponse)
     *
     * 로직:
     * 1. 아이디 공백 검사
     * 2. 학교 ID 유효성 검사 (존재하지 않는 학교면 예외 발생)
     * 3. 해당 학교 내에서 아이디 중복 검사
     * 4. 학생 엔티티 생성, 데이터 매핑, 비밀번호 암호화 후 저장
     */
    @Transactional
    public SignUpResponse signupStudent(SignUpRequest request) {

        Long requestSchoolId = request.getSchoolId();
        String username = request.getUsername() == null ? "" : request.getUsername().trim();

        // 1. 아이디 공백 체크
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디는 공백일 수 없습니다.");
        }

        // 2. 학교 존재 여부 확인
        // 학생은 이미 영양사가 등록해둔 학교(DB)에 소속되어야 하므로 조회가 필수입니다.
        School school = schoolRepository.findById(requestSchoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 학교 ID입니다."));

        // 3. 중복 아이디 체크 (해당 학교 내에서 유일해야 함)
        if (studentRepository.existsBySchoolAndUsername(school, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 학교에 이미 존재하는 아이디입니다.");
        }

        // 4. 엔티티 생성 및 데이터 세팅
        Student student = new Student();
        student.setSchool(school);          // 조회한 학교 엔티티 연결 (FK 설정)
        student.setUsername(username);
        student.setName(request.getName());
        student.setPhone(request.getPhone());
        student.setGrade(request.getGrade());
        student.setClassNo(request.getClassNo());

        // 알레르기 리스트([1,2]) -> CSV 문자열("1,2")로 변환하여 저장
        student.setAllergyCodes(toAllergyCsv(request.getAllergyCodes()));

        // 5. 비밀번호 암호화 및 저장
        student.setPasswordHash(passwordEncoder.encode(request.getPw()));
        Student saved = studentRepository.save(student);

        return new SignUpResponse(saved);
    }

    // =========================================================================
    // 2. 영양사 회원가입 (학교 정보 포함)
    // =========================================================================
    /**
     * [영양사 회원가입 및 학교 등록]
     *
     * @param request 영양사 정보 + 학교 정보(school_info)
     * @return 가입된 영양사 및 학교 정보 (DietitianSignUpResponse)
     *
     * 로직:
     * 1. 영양사 아이디 중복 확인
     * 2. 영양사 엔티티 생성 및 저장 (전화번호 포함)
     * 3. 학교 정보 중복 확인 (이미 등록된 표준학교코드인지 확인)
     * 4. 학교 엔티티 생성 및 저장 (영양사와 1:1 연관관계 설정)
     */
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

        // [중요] DTO에서 받은 전화번호를 엔티티에 저장
        dietitian.setPhone(request.getPhone());

        Dietitian savedDietitian = dietitianRepository.save(dietitian);

        // 3. 학교 정보 처리 (요청 DTO 내부에 있는 school_info 추출)
        SchoolRequest schoolReq = request.getSchoolInfo();

        // 3-1. 학교 중복 가입 방지 (이미 시스템에 등록된 표준학교코드인지 확인)
        if (schoolRepository.findBySchoolCode(schoolReq.getSchoolCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 시스템에 등록된 학교입니다.");
        }

        // 3-2. 학교 엔티티 빌드 (영양사와 연관관계 설정)
        School school = School.builder()
                .dietitian(savedDietitian) // [핵심] 방금 저장된 영양사와 연결 (FK)
                .schoolName(schoolReq.getSchoolName())
                .regionCode(schoolReq.getRegionCode())
                .schoolCode(schoolReq.getSchoolCode())
                .address(schoolReq.getAddress())
                .schoolType(schoolReq.getSchoolType())
                .build();

        // 3-3. 학교 저장
        School savedSchool = schoolRepository.save(school);

        // 4. 결과 반환 (영양사 정보와 학교 정보를 모두 포함한 DTO 반환)
        return new DietitianSignUpResponse(savedDietitian, savedSchool);
    }

    // =========================================================================
    // 3. 학생 로그인
    // =========================================================================
    /**
     * [학생 로그인]
     *
     * @param request 아이디, 비밀번호
     * @return JWT 토큰 문자열 (Payload에 Role: ROLE_STUDENT 포함)
     *
     * 로직:
     * 1. 아이디로 학생 조회 (없으면 예외)
     * 2. 비밀번호 일치 여부 확인 (없으면 예외)
     * 3. 인증 성공 시 토큰 발급
     */
    @Transactional(readOnly = true)
    public String login(LoginRequest request) {

        // 1. 학생 조회
        Student student = studentRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 아이디입니다."));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPw(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }

        // 3. 토큰 생성 및 반환
        return jwtProvider.createToken(
                student.getId(),
                student.getUsername(),
                student.getSchoolId(), // 학생이 소속된 학교 ID
                "ROLE_STUDENT"         // 권한 부여
        );
    }

    // =========================================================================
    // 4. 영양사 로그인
    // =========================================================================
    /**
     * [영양사 로그인]
     *
     * @param request 아이디, 비밀번호
     * @return JWT 토큰 문자열 (Payload에 Role: ROLE_DIETITIAN 포함)
     *
     * 특징:
     * - 영양사가 관리하는 학교의 ID를 조회하여 토큰에 포함시킵니다.
     * - 학교가 아직 등록되지 않은 경우(예외 케이스) schoolId는 null이 될 수 있습니다.
     */
    @Transactional(readOnly = true)
    public String loginDietitian(LoginRequest request) {

        // 1. 영양사 조회
        Dietitian dietitian = dietitianRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 영양사 아이디입니다."));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPw(), dietitian.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }

        // 3. 학교 ID 조회 (영양사와 연결된 학교 찾기)
        School school = schoolRepository.findByDietitian_Id(dietitian.getId()).orElse(null);
        Long schoolId = (school != null) ? school.getId() : null;

        // 4. JWT 토큰 생성 (권한: ROLE_DIETITIAN)
        return jwtProvider.createToken(
                dietitian.getId(),
                dietitian.getUsername(),
                schoolId,
                "ROLE_DIETITIAN" // 영양사 권한 부여
        );
    }

    // =========================================================================
    // 5. 학생 정보 수정
    // =========================================================================
    /**
     * [학생 프로필 업데이트]
     *
     * @param studentId 대상 학생 PK
     * @param request 변경할 정보 (이름, 전화번호, 학년, 반, 알레르기)
     * @return 수정된 학생 정보 DTO
     */
    @Transactional
    public SignUpResponse updateStudentProfile(Long studentId, StudentUpdateRequest request) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생 정보를 찾을 수 없습니다."));

        // null이 아닌 필드만 업데이트 (Dynamic Update 효과)
        if (request.getName() != null) student.setName(request.getName());
        if (request.getPhone() != null) student.setPhone(request.getPhone());
        if (request.getGrade() != null) student.setGrade(request.getGrade());
        if (request.getClassNo() != null) student.setClassNo(request.getClassNo());

        // 알레르기 정보 업데이트 (List -> CSV 변환)
        if (request.getAllergyCodes() != null) {
            student.setAllergyCodes(toAllergyCsv(request.getAllergyCodes()));
        }

        return new SignUpResponse(student);
    }

    // =========================================================================
    // 6. 학생 비밀번호 변경
    // =========================================================================
    /**
     * [학생 비밀번호 변경]
     *
     * @param studentId 대상 학생 PK
     * @param request 현재 비밀번호, 새 비밀번호
     *
     * 로직:
     * 1. 현재 비밀번호 일치 여부 확인 (틀리면 예외)
     * 2. 새 비밀번호가 기존 비밀번호와 다른지 확인 (같으면 예외)
     * 3. 새 비밀번호 암호화 후 저장
     */
    @Transactional
    public void changeStudentPassword(Long studentId, PasswordChangeRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생 정보를 찾을 수 없습니다."));

        // 1. 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }

        // 2. 새 비밀번호 중복 검사 (보안 권장사항)
        if (passwordEncoder.matches(request.getNewPassword(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새로운 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        // 3. 변경 및 저장 (Dirty Checking으로 자동 Update)
        student.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    }

    // =========================================================================
    // 유틸리티 메서드
    // =========================================================================
    /**
     * 알레르기 코드 리스트를 CSV 문자열로 변환하는 헬퍼 메서드
     * 예: [1, 3, 5] -> "1,3,5"
     * * - null이나 빈 리스트가 들어오면 빈 문자열 반환
     * - 1~19 사이의 유효한 코드만 필터링
     * - 중복 제거 및 정렬 수행
     */
    private String toAllergyCsv(List<Integer> codes) {
        if (codes == null || codes.isEmpty()) return "";
        return codes.stream()
                .filter(Objects::nonNull)
                .filter(c -> c >= 1 && c <= 19) // 1~19번 알레르기 코드만 유효
                .distinct() // 중복 제거
                .sorted()   // 오름차순 정렬
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}