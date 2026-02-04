package com.nutriassistant.nutriassistant_back.domain.School.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolSearchDto {
    // 1. 학교 식별 정보
    private Long schoolId;       // 우리 DB의 PK (null이면 미등록 학교)
    private String schoolCode;   // 표준 학교 코드
    private String regionCode;   // 교육청 코드

    // 2. 화면 표시 정보
    private String schoolName;   // 학교 이름
    private String address;      // ★ 주소 (동명이교 구분용)
    private String schoolType;   // 초/중/고

    // 3. 상태 정보 (프론트엔드 처리용)
    private boolean isRegistered; // true: 가입 가능(버튼 활성화), false: 가입 불가
    private String dietitianName; // 담당 영양사 이름 (있으면 표시)
    private String message;       // "가입 가능" or "미등록 학교" 텍스트
}