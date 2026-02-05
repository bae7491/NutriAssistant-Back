package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

// JSON 데이터 처리 및 유효성 검사를 위한 라이브러리 임포트
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nutriassistant.nutriassistant_back.domain.School.dto.SchoolRequest;
import com.nutriassistant.nutriassistant_back.global.validation.ValidPassword; // ✅ [수정] 커스텀 비밀번호 검증 어노테이션 임포트
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
// import jakarta.validation.constraints.Size; // [삭제] @ValidPassword가 길이 체크까지 수행하므로 삭제함

/**
 * [영양사 회원가입 요청 DTO]
 * 프론트엔드에서 영양사 가입 시 보내는 데이터를 담는 객체입니다.
 * 학교 정보(school_info)를 포함하고 있어, 가입과 동시에 학교를 등록합니다.
 */
public class DietitianSignUpRequest {

    // 아이디(username) 필드: 빈 값이나 공백을 허용하지 않습니다.
    @NotBlank(message = "아이디는 필수입니다.")
    private String username;

    // 비밀번호(pw) 필드: 빈 값 불가 + ✅ [수정] 복잡한 비밀번호 정책 적용
    @NotBlank(message = "비밀번호는 필수입니다.")
    @ValidPassword // ✅ 기존 @Size 대신 이 어노테이션을 사용하여 (영문/숫자/특수문자 조합 및 길이, 금지 문자)를 검증합니다.
    private String pw;

    // 이름(name) 필드: 필수 입력값입니다.
    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    // 이메일(email) 필드: 필수 입력값입니다.
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    // 전화번호(phone) 필드: 필수 입력값입니다.
    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    // 학교 정보(schoolInfo) 필드: 영양사는 가입 시 학교 정보가 반드시 있어야 합니다.
    // @Valid: SchoolRequest 내부의 필드(학교명, 코드 등)도 같이 검증하겠다는 의미입니다.
    @Valid
    @NotNull(message = "학교 정보는 필수입니다.")
    @JsonProperty("school_info") // JSON에서 "school_info"라는 키로 들어온 데이터를 여기에 매핑합니다.
    private SchoolRequest schoolInfo;

    // 기본 생성자 (Jackson 라이브러리가 역직렬화할 때 사용)
    public DietitianSignUpRequest() {}

    // =========================================================================
    // Getters & Setters (데이터 접근 및 수정 메서드)
    // =========================================================================

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPw() { return pw; }
    public void setPw(String pw) { this.pw = pw; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // 학교 정보 Getter/Setter
    public SchoolRequest getSchoolInfo() { return schoolInfo; }
    public void setSchoolInfo(SchoolRequest schoolInfo) { this.schoolInfo = schoolInfo; }

    /**
     * [편의 메서드]
     * Service 계층에서 getSchool()이라는 이름으로 호출할 때를 대비해
     * schoolInfo를 그대로 반환해줍니다. (호환성 유지)
     */
    public SchoolRequest getSchool() {
        return this.schoolInfo;
    }
}