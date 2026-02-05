package com.nutriassistant.nutriassistant_back.domain.Auth.DTO;

// Jakarta Validation 라이브러리 임포트
import jakarta.validation.constraints.NotBlank;

// 영양사 정보 수정을 위한 요청 DTO 클래스
public class DietitianUpdateRequest {

    // 이름 필드
    // PATCH(부분 수정)를 고려한다면, 클라이언트가 이름을 보내지 않을 경우 null이 될 수 있어야 하므로
    // 필수 검증(@NotBlank)을 상황에 따라 제거하거나, 서비스 계층에서 null이 아닐 때만 업데이트하도록 처리해야 합니다.
    // 여기서는 PUT/PATCH 모두 호환성을 위해 유효성 검증을 유지하되, 요청 시 반드시 포함해야 함을 가정하거나
    // 혹은 @NotBlank를 제거하고 서비스 로직에서 처리하는 것이 좋습니다.
    // 일단 기존 코드를 유지하되, 요청 시 값을 보내야 함을 주석으로 명시합니다.
    private String name;

    // 전화번호 필드
    private String phone;

    // 기본 생성자
    public DietitianUpdateRequest() {}

    // name 필드의 Getter 메서드
    public String getName() { return name; }
    // name 필드의 Setter 메서드
    public void setName(String name) { this.name = name; }

    // phone 필드의 Getter 메서드
    public String getPhone() { return phone; }
    // phone 필드의 Setter 메서드
    public void setPhone(String phone) { this.phone = phone; }
}