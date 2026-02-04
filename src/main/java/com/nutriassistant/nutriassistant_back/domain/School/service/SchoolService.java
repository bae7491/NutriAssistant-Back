package com.nutriassistant.nutriassistant_back.domain.School.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository; // ※ Dietitian 레포지토리가 필요합니다.
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolResponse;
import com.nutriassistant.nutriassistant_back.domain.School.entity.School;
import com.nutriassistant.nutriassistant_back.domain.School.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final DietitianRepository dietitianRepository; // 영양사 정보를 조회하기 위해 필요

    // =========================================================================
    // 1. 학교 등록 (나이스 검색 결과 -> 내 학교로 저장)
    // =========================================================================
    @Transactional
    public SchoolResponse registerSchool(Long dietitianId, SchoolRequest request) {
        log.info("학교 등록 요청 - dietitianId: {}, schoolName: {}", dietitianId, request.getSchoolName());

        // 1) 영양사 존재 확인
        Dietitian dietitian = dietitianRepository.findById(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 영양사입니다."));

        // 2) 이미 학교가 등록된 영양사인지 확인 (1인 1학교 정책)
        if (schoolRepository.findByDietitian_Id(dietitianId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 학교가 있습니다.");
        }

        // 3) (선택) 해당 학교 코드로 이미 등록된 학교가 있는지 중복 체크
        //    정책에 따라: 한 학교에 여러 영양사가 있을 수 있다면 이 체크는 생략 가능
        if (schoolRepository.findBySchoolCode(request.getSchoolCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 영양사에 의해 등록된 학교입니다.");
        }

        // 4) Entity 빌드
        School school = School.builder()
                .dietitian(dietitian) // 연관관계 설정
                .schoolName(request.getSchoolName())
                .regionCode(request.getRegionCode())
                .schoolCode(request.getSchoolCode())
                .address(request.getAddress())
                // 초기 설정값들 (request에 null로 오면 null로 저장됨)
                .schoolType(request.getSchoolType())
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();

        // 5) 저장
        School savedSchool = schoolRepository.save(school);

        return new SchoolResponse(savedSchool);
    }

    // =========================================================================
    // 2. 내 학교 정보 조회
    // =========================================================================
    @Transactional(readOnly = true)
    public SchoolResponse getMySchool(Long dietitianId) {
        School school = schoolRepository.findByDietitian_Id(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 학교 정보가 없습니다."));

        return new SchoolResponse(school);
    }

    // =========================================================================
    // 3. 학교 운영 정보 수정 (학생 수, 예산 등)
    // =========================================================================
    @Transactional
    public SchoolResponse updateSchoolInfo(Long dietitianId, SchoolRequest request) {
        // 1) 내 학교 찾기
        School school = schoolRepository.findByDietitian_Id(dietitianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 학교 정보가 없습니다."));

        // 2) 정보 업데이트 (Dirty Checking 활용)
        //    request의 값이 null이 아닐 때만 업데이트하거나, 덮어쓰기 정책 결정
        //    여기서는 DTO에 값이 있으면 덮어쓰는 방식으로 구현

        if (request.getStudentCount() != null) school.setStudentCount(request.getStudentCount());
        if (request.getTargetUnitPrice() != null) school.setTargetUnitPrice(request.getTargetUnitPrice());
        if (request.getMaxUnitPrice() != null) school.setMaxUnitPrice(request.getMaxUnitPrice());
        if (request.getOperationRules() != null) school.setOperationRules(request.getOperationRules());
        if (request.getCookWorkers() != null) school.setCookWorkers(request.getCookWorkers());
        if (request.getKitchenEquipment() != null) school.setKitchenEquipment(request.getKitchenEquipment());

        // 기본 정보도 수정 가능하게 할지 여부 (전화번호, 이메일 등)
        if (request.getPhone() != null) school.setPhone(request.getPhone());
        if (request.getEmail() != null) school.setEmail(request.getEmail());

        // 3) 저장된 상태 반환 (트랜잭션 종료 시 자동 update 쿼리 발생)
        return new SchoolResponse(school);
    }
}