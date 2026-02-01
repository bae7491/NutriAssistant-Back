package com.nutriassistant.nutriassistant_back.domain.Auth.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianSignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.DietitianUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.School;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.SchoolRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DietitianService {

    private final DietitianRepository dietitianRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;

    public DietitianService(DietitianRepository dietitianRepository,
                            SchoolRepository schoolRepository,
                            PasswordEncoder passwordEncoder) {
        this.dietitianRepository = dietitianRepository;
        this.schoolRepository = schoolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 영양사 회원가입 + 학교정보 기재(동시에 저장)
     */
    @Transactional
    public DietitianSignUpResponse signupWithSchool(DietitianSignUpRequest request) {

        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is blank");
        }

        if (dietitianRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 username 입니다.");
        }

        // 1) 영양사 저장
        Dietitian dietitian = new Dietitian();
        dietitian.setUsername(username);
        dietitian.setName(request.getName());
        dietitian.setPhone(request.getPhone());
        dietitian.setPasswordHash(passwordEncoder.encode(request.getPw()));

        Dietitian savedDietitian = dietitianRepository.save(dietitian);

        // 2) 학교 저장 (FK: dietitian_id)
        SchoolRequest sr = request.getSchool();

        School school = new School();
        school.setDietitian(savedDietitian);
        school.setSchoolName(sr.getSchoolName());
        school.setSchoolType(sr.getSchoolType());
        school.setPhone(sr.getPhone());
        school.setEmail(sr.getEmail());
        school.setStudentCount(sr.getStudentCount());
        school.setTargetUnitPrice(sr.getTargetUnitPrice());
        school.setMaxUnitPrice(sr.getMaxUnitPrice());
        school.setOperationRules(sr.getOperationRules());
        school.setCookWorkers(sr.getCookWorkers());
        school.setKitchenEquipment(sr.getKitchenEquipment());

        School savedSchool = schoolRepository.save(school);

        return new DietitianSignUpResponse(savedDietitian, savedSchool);
    }

    /**
     * ✅ 영양사 프로필 수정: name, phone만
     */
    @Transactional
    public DietitianProfileResponse updateDietitianProfile(Long dietitianId, DietitianUpdateRequest req) {

        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "dietitian not found"));

        dietitian.setName(req.getName());
        dietitian.setPhone(req.getPhone());

        Dietitian saved = dietitianRepository.save(dietitian);

        // 학교는 있으면 같이 내려주기(없어도 null로 처리)
        School school = schoolRepository.findByDietitian_Id(dietitianId).orElse(null);

        return new DietitianProfileResponse(saved, school);
    }

    /**
     * ✅ 영양사 비밀번호 변경: current_pw 검증 후 new_pw 저장
     */
    @Transactional
    public void changeDietitianPassword(Long dietitianId, PasswordChangeRequest req) {

        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "dietitian not found"));

        if (!passwordEncoder.matches(req.getCurrentPw(), dietitian.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }

        dietitian.setPasswordHash(passwordEncoder.encode(req.getNewPw()));
        dietitianRepository.save(dietitian);
    }

    /**
     * (옵션) 학교정보 수정/재기재: dietitianId 기준으로 있으면 업데이트, 없으면 생성
     */
    @Transactional
    public SchoolResponse upsertSchool(Long dietitianId, SchoolRequest req) {

        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "dietitian not found"));

        School school = schoolRepository.findByDietitian_Id(dietitianId).orElseGet(School::new);

        school.setDietitian(dietitian);
        school.setSchoolName(req.getSchoolName());
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
