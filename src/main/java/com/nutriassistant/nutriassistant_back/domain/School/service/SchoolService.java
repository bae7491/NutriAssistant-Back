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
    // - NEIS 데이터와 DB 데이터를 병합하여 DTO로 반환
    // =========================================================================
    @Transactional(readOnly = true)
    public List<SchoolSearchDto> searchSchoolsForUser(String keyword) {
        log.info("사용자용 학교 검색 요청 - keyword: {}", keyword);

        // 1) 나이스 API 호출
        List<NeisSchoolResponse.SchoolRow> neisRows = neisSchoolService.searchSchool(keyword);

        if (neisRows == null || neisRows.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) DTO 변환 및 DB 조회 매핑 (Stream API 사용)
        return neisRows.stream().map(row -> {
            // 학교 코드로 우리 DB 조회
            Optional<School> schoolOpt = schoolRepository.findBySchoolCode(row.getSchoolCode());

            if (schoolOpt.isPresent()) {
                // [CASE 1] DB에 학교가 존재함 (영양사 등록 여부 확인 필요)
                School school = schoolOpt.get();
                boolean hasDietitian = school.getDietitian() != null;

                return SchoolSearchDto.builder()
                        .schoolId(hasDietitian ? school.getId() : null) // 영양사 없으면 가입 불가하므로 ID null
                        .schoolCode(row.getSchoolCode())
                        .regionCode(row.getRegionCode())
                        .schoolName(row.getSchoolName())
                        .address(row.getAddress())
                        .schoolType(row.getSchoolType())
                        .isRegistered(hasDietitian) // 영양사가 있어야 true
                        .dietitianName(hasDietitian ? school.getDietitian().getName() : null)
                        .message(hasDietitian ? "가입 가능" : "담당 영양사 미배정")
                        .build();
            } else {
                // [CASE 2] DB에 학교가 없음 (미등록)
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
    // - 영양사는 등록을 위해 원본 데이터가 필요하므로 NEIS 결과 그대로 반환
    // =========================================================================
    @Transactional(readOnly = true)
    public List<NeisSchoolResponse.SchoolRow> searchSchools(String keyword) {
        return neisSchoolService.searchSchool(keyword);
    }

    // =========================================================================
    // 3. 학교 등록 (영양사)
    // - 기존 껍데기 학교가 있다면 정보를 업데이트하고 영양사를 매칭
    // =========================================================================
    @Transactional
    public SchoolResponse registerSchool(Long dietitianId, SchoolRequest request) {
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 영양사입니다."));

        // 1. 이미 내가 등록한 학교가 있는지 체크
        if (schoolRepository.findByDietitian_Id(dietitianId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 관리 중인 학교가 있습니다. 한 계정당 하나의 학교만 등록 가능합니다.");
        }

        // 2. 학교 코드로 이미 존재하는 학교인지 체크
        Optional<School> existingSchool = schoolRepository.findBySchoolCode(request.getSchoolCode());

        School school;
        if (existingSchool.isPresent()) {
            // [CASE A] 이미 존재하는 학교 (학생들이 껍데기를 만들어둔 경우 등)
            school = existingSchool.get();

            // 이미 주인이 있는지 확인
            if (school.getDietitian() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 영양사님이 관리 중인 학교입니다.");
            }

            // ★ 영양사 연결
            school.setDietitian(dietitian);

            // ★ 중요: 프론트에서 보내준 최신 정보로 덮어쓰기 (이사갔거나 교명 변경, 누락 정보 대비)
            school.setSchoolName(request.getSchoolName());
            school.setRegionCode(request.getRegionCode());
            school.setAddress(request.getAddress());
            school.setSchoolType(request.getSchoolType());
            school.setPhone(request.getPhone());
            school.setEmail(request.getEmail());

        } else {
            // [CASE B] 아예 없는 학교 -> 신규 생성
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
    // 5. 학교 정보 수정 (학생 수, 단가 등 운영 정보)
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