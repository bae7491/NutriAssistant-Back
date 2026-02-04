package com.nutriassistant.nutriassistant_back.domain.Auth.service;

// 패키지명 소문자 규칙 적용 (Auth -> auth, School -> school)
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 생성해줍니다.
public class DietitianService {

    private final DietitianRepository dietitianRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * ✅ [회원가입] 영양사 정보 생성 + 학교 정보 등록 (Transaction 필수)
     * - 영양사 계정을 먼저 만들고, 그 ID를 FK로 가지는 학교 정보를 저장합니다.
     * - 나이스(NEIS) API 연동을 위한 필수 코드(regionCode, schoolCode)도 함께 저장됩니다.
     */
    @Transactional
    public DietitianSignUpResponse signupWithSchool(DietitianSignUpRequest request) {

        // 1. Username 유효성 검사
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디(username)는 필수입니다.");
        }

        // 2. 중복 검사
        if (dietitianRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
        }

        // 3. 영양사(Dietitian) 엔티티 생성 및 저장
        Dietitian dietitian = new Dietitian();
        dietitian.setUsername(username);
        dietitian.setName(request.getName());
        dietitian.setPhone(request.getPhone());
        dietitian.setPasswordHash(passwordEncoder.encode(request.getPw())); // 비밀번호 암호화

        Dietitian savedDietitian = dietitianRepository.save(dietitian);

        // 4. 학교(School) 엔티티 생성 및 저장
        // SchoolRequest에서 학교 정보 + 나이스 코드를 꺼내옵니다.
        SchoolRequest sr = request.getSchool();
        School savedSchool = null;

        if (sr != null) {
            School school = new School();
            school.setDietitian(savedDietitian); // FK 설정

            // [중요] 나이스 API 연동 필수 데이터 매핑
            school.setSchoolName(sr.getSchoolName());
            school.setRegionCode(sr.getRegionCode()); // 시도교육청코드
            school.setSchoolCode(sr.getSchoolCode()); // 표준학교코드
            school.setAddress(sr.getAddress());       // 주소

            // 기타 운영 정보 매핑 (회원가입 시점에는 없을 수도 있음)
            school.setSchoolType(sr.getSchoolType());
            school.setPhone(sr.getPhone());
            school.setEmail(sr.getEmail());
            // 필요한 경우 나머지 필드도 매핑...

            savedSchool = schoolRepository.save(school);
        }

        // 5. 응답 DTO 생성 (DietitianSignUpResponse)
        return new DietitianSignUpResponse(savedDietitian, savedSchool);
    }

    /**
     * ✅ [프로필 수정] 영양사 개인정보(이름, 전화번호)만 수정
     */
    @Transactional
    public DietitianProfileResponse updateDietitianProfile(Long dietitianId, DietitianUpdateRequest req) {

        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        // 정보 업데이트 (Dirty Checking에 의해 Transaction 종료 시 자동 저장됨)
        dietitian.setName(req.getName());
        dietitian.setPhone(req.getPhone());

        // 명시적 저장을 선호한다면 save 호출 (JPA에서는 필수는 아님)
        Dietitian saved = dietitianRepository.save(dietitian);

        // 연관된 학교 정보 조회 (없을 경우 null 처리)
        School school = schoolRepository.findByDietitian_Id(dietitianId).orElse(null);

        return new DietitianProfileResponse(saved, school);
    }

    /**
     * ✅ [비밀번호 변경] 현재 비밀번호 확인 후 변경
     */
    @Transactional
    public void changeDietitianPassword(Long dietitianId, PasswordChangeRequest req) {

        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        // [수정] req.getCurrentPw() -> req.getCurrentPassword()로 변경
        if (!passwordEncoder.matches(req.getCurrentPassword(), dietitian.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }

        // [수정] req.getNewPw() -> req.getNewPassword()로 변경
        dietitian.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));

        // (참고: Dirty Checking으로 인해 명시적 save 없어도 트랜잭션 종료 시 업데이트되지만, 명시해도 무관함)
        dietitianRepository.save(dietitian);
    }

    /**
     * ✅ [학교 정보 등록/수정] (Upsert)
     * - 영양사가 이직하거나 학교 정보를 수정해야 할 때 사용
     * - 나이스 코드(regionCode, schoolCode) 수정 로직 포함
     */
    @Transactional
    public SchoolResponse upsertSchool(Long dietitianId, SchoolRequest req) {

        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영양사 정보를 찾을 수 없습니다."));

        // 기존 학교 정보가 있으면 가져오고, 없으면 새로 생성 (create)
        School school = schoolRepository.findByDietitian_Id(dietitianId).orElseGet(School::new);

        // 연관관계 설정
        if (school.getDietitian() == null) {
            school.setDietitian(dietitian);
        }

        // [중요] 나이스 정보 및 학교 기본 정보 업데이트
        school.setSchoolName(req.getSchoolName());
        school.setRegionCode(req.getRegionCode()); // 변경될 수 있으므로 업데이트
        school.setSchoolCode(req.getSchoolCode()); // 변경될 수 있으므로 업데이트
        school.setAddress(req.getAddress());

        // 상세 운영 정보 업데이트
        school.setSchoolType(req.getSchoolType());
        school.setPhone(req.getPhone());
        school.setEmail(req.getEmail());
        school.setStudentCount(req.getStudentCount());
        school.setTargetUnitPrice(req.getTargetUnitPrice());
        school.setMaxUnitPrice(req.getMaxUnitPrice());
        school.setOperationRules(req.getOperationRules());
        school.setCookWorkers(req.getCookWorkers());
        school.setKitchenEquipment(req.getKitchenEquipment());

        School saved = schoolRepository.save(school);
        return new SchoolResponse(saved);
    }
}