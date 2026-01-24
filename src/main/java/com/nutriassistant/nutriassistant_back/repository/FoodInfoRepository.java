package com.nutriassistant.nutriassistant_back.repository;

import com.nutriassistant.nutriassistant_back.entity.FoodInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // [추가됨] 쿼리 어노테이션
import org.springframework.data.repository.query.Param; // [추가됨] 파라미터 바인딩

import java.time.Instant;
import java.util.Optional; // [추가됨] Null 처리를 위해 필요

public interface FoodInfoRepository extends JpaRepository<FoodInfo, Long> {

    // [기존 코드 유지] 동기화 또는 목록 조회용 메서드들
    Page<FoodInfo> findByUpdatedAtAfter(Instant since, Pageable pageable);
    Page<FoodInfo> findAll(Pageable pageable);

    // =========================================================================
    // [추가됨] 수동 수정 시 메뉴 매칭을 위한 핵심 메서드
    // =========================================================================
    // 설명: DB의 food_name과 입력된 foodName의 공백을 모두 제거(REPLACE)한 뒤 비교합니다.
    // 효과: 사용자가 "된장 국"이라고 입력해도 DB의 "된장국"을 찾을 수 있습니다.
    // [수정] nativeQuery = true 옵션을 추가하고, 실제 테이블명(food_info)과 컬럼명(food_name)을 사용
    // 이걸 복사해서 Repository에 덮어씌우세요
    @Query(value = "SELECT * FROM food_info WHERE REPLACE(food_name, ' ', '') = REPLACE(:foodName, ' ', '')", nativeQuery = true)
    Optional<FoodInfo> findByFoodNameIgnoreSpace(@Param("foodName") String foodName);
}
