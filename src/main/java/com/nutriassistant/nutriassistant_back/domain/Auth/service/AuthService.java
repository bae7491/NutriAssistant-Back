package com.nutriassistant.nutriassistant_back.domain.Auth.service;

import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SignUpRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.SignUpResponse;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.PasswordChangeRequest;
import com.nutriassistant.nutriassistant_back.domain.Auth.DTO.StudentUpdateRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignUpResponse signupStudent(SignUpRequest request) {

        Long schoolId = request.getSchoolId();
        String username = request.getUsername() == null ? "" : request.getUsername().trim();

        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is blank");
        }

        // (school_id + username) 중복 체크
        if (studentRepository.existsBySchoolIdAndUsername(schoolId, username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 username 입니다.");
        }

        Student student = new Student();
        student.setSchoolId(schoolId);
        student.setUsername(username);
        student.setName(request.getName());
        student.setPhone(request.getPhone());
        student.setGrade(request.getGrade());
        student.setClassNo(request.getClassNo());
        student.setAllergyCodes(toAllergyCsv(request.getAllergyCodes()));

        // pw -> BCrypt 해시 -> password_hash 저장
        student.setPasswordHash(passwordEncoder.encode(request.getPw()));

        Student saved = studentRepository.save(student);

        // ✅ 여기! Student를 그대로 넘겨야 함
        return new SignUpResponse(saved);
    }
    @Transactional
    public SignUpResponse updateStudentProfile(Long studentId, StudentUpdateRequest request) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "student not found"));

        // ✅ 수정 가능 필드만 변경
        student.setName(request.getName());
        student.setPhone(request.getPhone());
        student.setGrade(request.getGrade());
        student.setClassNo(request.getClassNo());

        Student saved = studentRepository.save(student);
        return new SignUpResponse(saved);
    }

    @Transactional
    public void changeStudentPassword(Long studentId, PasswordChangeRequest request) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "student not found"));

        // ✅ 현재 비번 검증
        if (!passwordEncoder.matches(request.getCurrentPw(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }

        // ✅ 새 비번 저장
        student.setPasswordHash(passwordEncoder.encode(request.getNewPw()));
        studentRepository.save(student);
    }

    private Set<Integer> validateAllergyCodes(List<Integer> codes) {
        if (codes == null) return Set.of();

        Set<Integer> out = new HashSet<>();
        for (Integer c : codes) {
            if (c == null) continue;
            if (c < 1 || c > 19) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid allergy code: " + c);
            }
            out.add(c); // 중복 자동 제거
        }
        return out;
    }

    private String toAllergyCsv(List<Integer> codes) {
        if (codes == null || codes.isEmpty()) return "";
        return codes.stream()
                .filter(Objects::nonNull)
                .filter(c -> c >= 1 && c <= 19) // 1~19만 허용
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }


}
