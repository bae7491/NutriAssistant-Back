package com.nutriassistant.nutriassistant_back.domain.School.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.School.dto.NeisSchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolSearchDto;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.domain.School.repository.SchoolRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
    // - 등록된 학교만 가입 가능하도록 식별 플래그(registered) 설정
    // =========================================================================
    @Transactional(readOnly = true)
    public List<SchoolSearchDto> searchSchoolsForUser(String keyword) {
        log.info("사용자용 학교 검색: {}", keyword);

        // 1. 나이스 API에서 전체 학교 목록 가져오기
        List<NeisSchoolResponse.SchoolRow> neisList = neisSchoolService.searchSchool(keyword);

        if (neisList == null || neisList.isEmpty()) {
            return Collections.emptyList();
        }

        List<SchoolSearchDto> resultList = new ArrayList<>();

        // 2. 각 학교가 우리 서비스(DB)에 등록되어 있는지 확인
        for (NeisSchoolResponse.SchoolRow row : neisList) {

            // 기본 DTO 생성 (주소 포함하여 동명이교 구분)
            SchoolSearchDto dto = SchoolSearchDto.builder()
                    .schoolName(row.getSchoolName())
                    .schoolCode(row.getSchoolCode())
                    .regionCode(row.getRegionCode())
                    .address(row.getAddress())
                    .schoolType(row.getSchoolType())
                    .build();

            // DB 조회 (표준 학교 코드로 확인)
            Optional<School> existingSchool = schoolRepository.findBySchoolCode(row.getSchoolCode());

            // ★ 가입 가능 여부 판별 로직 ★
            if (existingSchool.isPresent() && existingSchool.get().getDietitian() != null) {
                // CASE 1: 학교가 있고, 담당 영양사도 있음 -> [가입 가능]
                School school = existingSchool.get();
                dto.setSchoolId(school.getId());
                dto.setRegistered(true); // 프론트: 활성화
                dto.setDietitianName(school.getDietitian().getName());
                dto.setMessage("가입 가능");
            } else {
                // CASE 2: DB에 없거나, 영양사가 배정되지 않음 -> [가입 불가]
                dto.setSchoolId(null);   // ID가 없으므로 가입 요청 불가능
                dto.setRegistered(false); // 프론트: 비활성화(Gray out)
                dto.setMessage("미등록 학교 (가입 불가)");
            }

            resultList.add(dto);
        }

        return resultList;
    }

    // =========================================================================
    // 2. [영양사 등록용] 학교 검색
    // =========================================================================
    @Transactional(readOnly = true)
    public List<NeisSchoolResponse.SchoolRow> searchSchools(String keyword) {
        // 영양사는 미등록 학교를 찾아야 하므로 NEIS 결과 그대로 반환
        return neisSchoolService.searchSchool(keyword);
    }

    // =========================================================================
    // 3. 학교 등록 (영양사)
    // =========================================================================
    @Transactional
    public SchoolResponse registerSchool(Long dietitianId, SchoolRequest request) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 영양사입니다."));

        // 해당 영양사가 이미 등록한 학교가 있는지 확인
        if (schoolRepository.findByDietitian_Id(dietitianId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 관리 중인 학교가 있습니다. 한 계정당 하나의 학교만 등록 가능합니다.");
        }

        // 해당 학교가 이미 다른 영양사에 의해 등록되었는지 확인
        Optional<School> existingSchool = schoolRepository.findBySchoolCode(request.getSchoolCode());

        School school;
        if (existingSchool.isPresent()) {
            school = existingSchool.get();
            if (school.getDietitian() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 영양사님이 관리 중인 학교입니다.");
            }
            // (혹시 데이터만 있고 주인 없는 학교라면 내 학교로 등록)
            school.setDietitian(dietitian);
            // 필요한 경우 정보 업데이트 코드 추가
        } else {
            // 신규 등록
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
    // 5. 학교 정보 수정
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