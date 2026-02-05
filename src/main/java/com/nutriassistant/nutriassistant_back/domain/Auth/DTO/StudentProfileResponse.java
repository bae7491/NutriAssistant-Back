package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentProfileResponse {

    private Long id;
    private String studentName;
    private String loginId;
    private String schoolName;
    private String email;

    // Entity -> DTO 변환 메서드
    public static StudentProfileResponse from(Student student) {
        return StudentProfileResponse.builder()
                .id(student.getId())
                .studentName(student.getName()) // Student 엔티티에 getName()이 있다고 가정
                .loginId(student.getUsername()) // Student 엔티티에 getUsername()이 있다고 가정
                .schoolName(student.getSchool() != null ? student.getSchool().getSchoolName() : null)
                .email(student.getEmail())      // Student 엔티티에 getEmail()이 있다고 가정
                .build();
    }
}