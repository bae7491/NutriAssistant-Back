package com.nutriassistant.nutriassistant_back.domain.Auth.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.StudentProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.StudentRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /*
     * [학생 프로필 조회]
     * 학생 ID를 기반으로 학생 정보를 조회하여 DTO로 반환합니다.
     */
    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // DTO 변환 (from 메서드나 생성자를 활용한다고 가정)
        return StudentProfileResponse.from(student);
    }

    /*
     * [회원 탈퇴]
     * 학생의 상태를 탈퇴(WITHDRAWN)로 변경하고,
     * 보안을 위해 리프레시 토큰을 삭제합니다.
     */
    public void withdraw(Long studentId) {
        // 1. 학생 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 상태 변경 (Soft Delete: ACTIVE -> WITHDRAWN)
        student.withdraw();

        // 3. 리프레시 토큰 삭제 (로그인 차단)
        refreshTokenRepository.deleteByUsername(student.getUsername());
    }
}