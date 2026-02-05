package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

// JSON 처리 및 유효성 검사 라이브러리 임포트
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.global.validation.ValidPassword; // ✅ [수정] 커스텀 비밀번호 검증 어노테이션 임포트
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * [학생 회원가입 요청 DTO]
 * 학생이 회원가입할 때 프론트엔드에서 보내는 데이터를 받습니다.
 * snake_case("school_id")와 camelCase("schoolId")를 모두 허용하도록 설정되어 있습니다.
 */
public class SignUpRequest {

    // 학교 ID: 학생이 소속될 학교의 고유 ID (필수)
    @NotNull
    @JsonProperty("school_id")    // JSON으로 보낼 때 표준 키 이름
    @JsonAlias({"schoolId"})      // 프론트가 실수로 schoolId로 보내도 받아줌
    private Long schoolId;

    // 아이디: 최대 255자까지 허용
    @NotBlank
    @Size(max = 255)
    @JsonProperty("username")
    @JsonAlias({"email"})         // 혹시 이메일을 아이디로 쓰는 경우를 대비해 별칭 허용
    private String username;

    // 비밀번호: ✅ [수정] 복잡한 비밀번호 정책 적용
    @NotBlank
    // @Size(min = 8, max = 72) // [삭제] 단순 길이 체크 제거
    @ValidPassword // ✅ 여기에 커스텀 어노테이션 적용 (금지문자, 조합 규칙 등 검사)
    @JsonProperty("pw")
    @JsonAlias({"password"})      // password라는 키로 와도 매핑됨
    private String pw;

    // 이름: 최대 100자
    @NotBlank
    @Size(max = 100)
    @JsonProperty("name")
    private String name;

    // 전화번호: 최대 50자
    @NotBlank
    @Size(max = 50)
    @JsonProperty("phone")
    private String phone;

    // 학년: 필수 입력
    @NotNull
    @JsonProperty("grade")
    private Integer grade;

    // 반: 필수 입력 (프론트에서 classNo 또는 classroom으로 보내도 됨)
    @NotNull
    @JsonProperty("class_no")
    @JsonAlias({"classNo", "classroom"})
    private Integer classNo;

    /**
     * 알레르기 코드 리스트 (선택사항)
     * 예: [1, 5, 12] 와 같이 배열 형태로 들어옵니다.
     * Service 계층에서 이를 "1,5,12" 문자열로 변환하여 DB에 저장합니다.
     */
    @JsonProperty("allergy_codes")
    @JsonAlias({"allergyCodes", "allergies"})
    private List<Integer> allergyCodes;

    // 기본 생성자
    public SignUpRequest() {}

    // =========================================================================
    // Getters & Setters
    // =========================================================================

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPw() { return pw; }
    public void setPw(String pw) { this.pw = pw; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }

    public Integer getClassNo() { return classNo; }
    public void setClassNo(Integer classNo) { this.classNo = classNo; }

    public List<Integer> getAllergyCodes() { return allergyCodes; }
    public void setAllergyCodes(List<Integer> allergyCodes) { this.allergyCodes = allergyCodes; }
}