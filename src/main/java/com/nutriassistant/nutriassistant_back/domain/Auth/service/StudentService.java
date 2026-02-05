package com.nutriassistant.nutriassistant_back.domain.Auth.service;

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

    // [회원 탈퇴]
    public void withdraw(Long studentId) {
        // 1. 학생 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 상태 변경 (Soft Delete: ACTIVE -> WITHDRAWN)
        student.withdraw();

        // 3. 리프레시 토큰 삭제 (로그인 차단)
        // [수정] Student 엔티티에는 email이 없으므로 username을 사용해야 합니다.
        refreshTokenRepository.deleteByUsername(student.getUsername());
    }
}