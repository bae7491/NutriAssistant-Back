package com.nutriassistant.nutriassistant_back.domain.Auth.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.StudentProfileResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.RefreshTokenRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    // [추가] 비밀번호 암호화 및 검증을 위한 인코더 주입
    private final PasswordEncoder passwordEncoder;

    /*
     * [학생 프로필 조회]
     * 학생 ID를 기반으로 학생 정보를 조회하여 DTO로 반환합니다.
     */
    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return StudentProfileResponse.from(student);
    }

    /*
     * [비밀번호 변경] - 신규 추가됨
     * 1. 현재 비밀번호가 맞는지 확인합니다.
     * 2. 틀리면 예외를 발생시킵니다.
     * 3. 맞으면 새 비밀번호를 암호화하여 저장(Update)합니다.
     */
    public void changePassword(Long studentId, PasswordChangeRequest request) {
        // 1. 사용자 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 현재 비밀번호 검증 (입력한 평문 vs DB에 저장된 암호문 비교)
        if (!passwordEncoder.matches(request.getCurrentPassword(), student.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 3. 새 비밀번호 암호화 (BCrypt 등 설정된 방식으로 인코딩)
        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());

        // 4. 정보 업데이트 (Dirty Checking으로 인해 트랜잭션 종료 시 자동 UPDATE 쿼리 발생)
        student.setPasswordHash(encodedNewPassword);
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