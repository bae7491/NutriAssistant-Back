package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 학생 프로필 정보를 상세히 응답하기 위한 DTO 클래스입니다.
 * 프런트엔드에서 수정 페이지 진입 시 기존 데이터를 불러오는 데 사용됩니다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentProfileResponse {

    private Long id;

    // 학생의 실명
    private String studentName;

    // 로그인에 사용되는 계정 ID (username)
    private String loginId;

    // 현재 소속된 학교 이름
    private String schoolName;

    // 이메일 (계정 ID와 동일하게 설정됨)
    private String email;

    // 연락처 정보
    private String phone;

    // 학년 정보
    private Integer grade;

    // 학급 정보 (JSON 출력 시 class_no로 매핑)
    @JsonProperty("class_no")
    private Integer classNo;

    // 알레르기 코드 리스트 (String으로 저장된 코드를 리스트 형태로 변환하여 응답)
    @JsonProperty("allergy_codes")
    private List<Integer> allergyCodes;

    /**
     * Student 엔티티 객체를 StudentProfileResponse DTO로 변환하는 정적 팩토리 메서드입니다.
     *
     * @param student 변환할 학생 엔티티 객체
     * @return 필드 값이 채워진 DTO 객체
     */
    public static StudentProfileResponse from(Student student) {
        // String 타입의 allergyCodes("1,2,3")를 List<Integer> 형태로 변환하는 로직
        List<Integer> allergyList = null;
        if (student.getAllergyCodes() != null && !student.getAllergyCodes().isEmpty()) {
            allergyList = Arrays.stream(student.getAllergyCodes().split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }

        return StudentProfileResponse.builder()
                .id(student.getId())
                .studentName(student.getName())
                .loginId(student.getUsername())
                .schoolName(student.getSchool() != null ? student.getSchool().getSchoolName() : null)
                .email(student.getEmail())
                .phone(student.getPhone())      // 엔티티의 전화번호 추가
                .grade(student.getGrade())      // 엔티티의 학년 추가
                .classNo(student.getClassNo())  // 엔티티의 반 정보 추가
                .allergyCodes(allergyList)      // 변환된 알레르기 리스트 추가
                .build();
    }
}