package com.nutriassistant.nutriassistant_back.domain.School.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.School.dto.NeisSchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.domain.School.repository.SchoolRepository;

// [수정됨] NeisSchoolService가 같은 패키지(domain.School.service)에 있으므로 import 제거
// import com.nutriassistant.nutriassistant_back.global.neis.NeisSchoolService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final DietitianRepository dietitianRepository;
    private final NeisSchoolService neisSchoolService; // 같은 패키지라 바로 사용 가능

    // =========================================================================
    // 1. 학교 검색 (나이스 API + DB 매핑)
    // =========================================================================
    @Transactional(readOnly = true)
    public List<NeisSchoolResponse.SchoolRow> searchSchools(String keyword) {
        log.info("학교 검색 요청 - keyword: {}", keyword);

        // 1) 나이스 API 호출 (NeisSchoolService 사용)
        NeisSchoolResponse neisResponse = (NeisSchoolResponse) neisSchoolService.searchSchool(keyword);

        if (neisResponse == null || neisResponse.getSchoolInfo() == null) {
            return Collections.emptyList();
        }

        List<NeisSchoolResponse.SchoolRow> rows = neisResponse.getSchoolInfo().get(0).getRow();

        // 2) 우리 DB에 등록된 학교인지 확인 (ID 매핑)
        for (NeisSchoolResponse.SchoolRow row : rows) {
            Optional<School> existingSchool = schoolRepository.findBySchoolCode(row.getSchoolCode());

            if (existingSchool.isPresent()) {
                row.setSchoolId(existingSchool.get().getId()); // ID 채워넣기
            } else {
                row.setSchoolId(null);
            }
        }

        return rows;
    }

    // =========================================================================
    // 2. 학교 등록 (영양사 회원가입 시 등)
    // =========================================================================
    @Transactional
    public SchoolResponse registerSchool(Long dietitianId, SchoolRequest request) {
        log.info("학교 등록 요청 - dietitianId: {}, schoolName: {}", dietitianId, request.getSchoolName());

        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 영양사입니다."));

        if (schoolRepository.findByDietitian_Id(dietitianId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 학교가 있습니다.");
        }

        if (schoolRepository.findBySchoolCode(request.getSchoolCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 영양사에 의해 등록된 학교입니다.");
        }

        School school = School.builder()
                .dietitian(dietitian)
                .schoolName(request.getSchoolName())
                .regionCode(request.getRegionCode())
                .schoolCode(request.getSchoolCode())
                .address(request.getAddress())
                .schoolType(request.getSchoolType())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        School savedSchool = schoolRepository.save(school);
        return new SchoolResponse(savedSchool);
    }

    // =========================================================================
    // 3. 내 학교 정보 조회
    // =========================================================================
    @Transactional(readOnly = true)
    public SchoolResponse getMySchool(Long dietitianId) {
        School school = schoolRepository.findByDietitian_Id(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 학교 정보가 없습니다."));

        return new SchoolResponse(school);
    }

    // =========================================================================
    // 4. 학교 정보 수정 (학생 수, 단가 등)
    // =========================================================================
    @Transactional
    public SchoolResponse updateSchoolInfo(Long dietitianId, SchoolRequest request) {
        School school = schoolRepository.findByDietitian_Id(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 학교 정보가 없습니다."));

        if (request.getStudentCount() != null) school.setStudentCount(request.getStudentCount());
        if (request.getTargetUnitPrice() != null) school.setTargetUnitPrice(request.getTargetUnitPrice());
        if (request.getMaxUnitPrice() != null) school.setMaxUnitPrice(request.getMaxUnitPrice());
        if (request.getOperationRules() != null) school.setOperationRules(request.getOperationRules());
        if (request.getCookWorkers() != null) school.setCookWorkers(request.getCookWorkers());
        if (request.getKitchenEquipment() != null) school.setKitchenEquipment(request.getKitchenEquipment());
        if (request.getPhone() != null) school.setPhone(request.getPhone());
        if (request.getEmail() != null) school.setEmail(request.getEmail());

        return new SchoolResponse(school);
    }
}