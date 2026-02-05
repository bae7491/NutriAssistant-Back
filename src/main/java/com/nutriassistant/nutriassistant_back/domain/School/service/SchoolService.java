package com.nutriassistant.nutriassistant_back.domain.School.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.School.dto.NeisSchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolSearchDto;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolUpdateRequest; // ✅ 수정용 DTO 임포트
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.domain.School.repository.SchoolRepository;

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
    private final NeisSchoolService neisSchoolService;

    // =========================================================================
    // 1. [학생 가입용] 학교 검색
    // =========================================================================
    @Transactional(readOnly = true)
    public List<SchoolSearchDto> searchSchoolsForUser(String keyword) {
        log.info("사용자용 학교 검색 요청 - keyword: {}", keyword);
        List<NeisSchoolResponse.SchoolRow> neisRows = neisSchoolService.searchSchool(keyword);

        if (neisRows == null || neisRows.isEmpty()) {
            return Collections.emptyList();
        }

        return neisRows.stream().map(row -> {
            Optional<School> schoolOpt = schoolRepository.findBySchoolCode(row.getSchoolCode());

            if (schoolOpt.isPresent()) {
                School school = schoolOpt.get();
                boolean hasDietitian = school.getDietitian() != null;

                return SchoolSearchDto.builder()
                        .schoolId(hasDietitian ? school.getId() : null)
                        .schoolCode(row.getSchoolCode())
                        .regionCode(row.getRegionCode())
                        .schoolName(row.getSchoolName())
                        .address(row.getAddress())
                        .schoolType(row.getSchoolType())
                        .isRegistered(hasDietitian)
                        .dietitianName(hasDietitian ? school.getDietitian().getName() : null)
                        .message(hasDietitian ? "가입 가능" : "담당 영양사 미배정")
                        .build();
            } else {
                return SchoolSearchDto.builder()
                        .schoolId(null)
                        .schoolCode(row.getSchoolCode())
                        .regionCode(row.getRegionCode())
                        .schoolName(row.getSchoolName())
                        .address(row.getAddress())
                        .schoolType(row.getSchoolType())
                        .isRegistered(false)
                        .dietitianName(null)
                        .message("미등록 학교")
                        .build();
            }
        }).toList();
    }

    // =========================================================================
    // 2. [영양사 등록용] 학교 검색
    // =========================================================================
    @Transactional(readOnly = true)
    public List<NeisSchoolResponse.SchoolRow> searchSchools(String keyword) {
        return neisSchoolService.searchSchool(keyword);
    }

    // =========================================================================
    // 3. 학교 등록 (영양사)
    // =========================================================================
    @Transactional
    public SchoolResponse registerSchool(Long dietitianId, SchoolRequest request) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 영양사입니다."));

        if (schoolRepository.findByDietitian_Id(dietitianId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 관리 중인 학교가 있습니다.");
        }

        Optional<School> existingSchool = schoolRepository.findBySchoolCode(request.getSchoolCode());
        School school;

        if (existingSchool.isPresent()) {
            school = existingSchool.get();
            if (school.getDietitian() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 영양사님이 관리 중인 학교입니다.");
            }
            school.setDietitian(dietitian);
            school.setSchoolName(request.getSchoolName());
            school.setRegionCode(request.getRegionCode());
            school.setAddress(request.getAddress());
            school.setSchoolType(request.getSchoolType());
            school.setPhone(request.getPhone());
            school.setEmail(request.getEmail());
        } else {
            school = School.builder()
                    .dietitian(dietitian)
                    .schoolName(request.getSchoolName())
                    .regionCode(request.getRegionCode())
                    .schoolCode(request.getSchoolCode())
                    .address(request.getAddress())
                    .schoolType(request.getSchoolType())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .build();
            schoolRepository.save(school);
        }
        return new SchoolResponse(school);
    }

    // =========================================================================
    // 4. 내 학교 조회
    // =========================================================================
    @Transactional(readOnly = true)
    public SchoolResponse getMySchool(Long dietitianId) {
        School school = schoolRepository.findByDietitian_Id(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 학교 정보가 없습니다."));
        return new SchoolResponse(school);
    }

    // =========================================================================
    // 5. 학교 정보 수정 (운영 데이터 포함) - SchoolUpdateRequest 사용!
    // =========================================================================
    @Transactional
    public SchoolResponse updateSchoolInfo(Long dietitianId, SchoolUpdateRequest request) {
        School school = schoolRepository.findByDietitian_Id(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 학교 정보가 없습니다."));

        // 1. 기본 정보 수정 (NEIS 정보가 바뀌었을 경우를 대비)
        if (request.getSchoolName() != null) school.setSchoolName(request.getSchoolName());
        if (request.getSchoolCode() != null) school.setSchoolCode(request.getSchoolCode());
        if (request.getRegionCode() != null) school.setRegionCode(request.getRegionCode());
        if (request.getAddress() != null) school.setAddress(request.getAddress());
        if (request.getSchoolType() != null) school.setSchoolType(request.getSchoolType());

        // 2. 운영 정보 및 연락처 수정
        if (request.getPhone() != null) school.setPhone(request.getPhone());
        if (request.getEmail() != null) school.setEmail(request.getEmail());

        // 3. 상세 운영 데이터 (학생 수, 단가, 인력, 기구 등)
        if (request.getStudentCount() != null) school.setStudentCount(request.getStudentCount());
        if (request.getTargetUnitPrice() != null) school.setTargetUnitPrice(request.getTargetUnitPrice());
        if (request.getMaxUnitPrice() != null) school.setMaxUnitPrice(request.getMaxUnitPrice());
        if (request.getOperationRules() != null) school.setOperationRules(request.getOperationRules());
        if (request.getCookWorkers() != null) school.setCookWorkers(request.getCookWorkers());
        if (request.getKitchenEquipment() != null) school.setKitchenEquipment(request.getKitchenEquipment());

        return new SchoolResponse(school);
    }
}