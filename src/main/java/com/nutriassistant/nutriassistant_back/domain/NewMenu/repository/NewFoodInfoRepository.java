package com.nutriassistant.nutriassistant_back.domain.NewMenu.repository;

import com.nutriassistant.nutriassistant_back.domain.NewMenu.entity.NewFoodInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewFoodInfoRepository extends JpaRepository<NewFoodInfo, Long> {

    Optional<NewFoodInfo> findByFoodCode(String foodCode);

    // schoolId + foodCode로 조회
    Optional<NewFoodInfo> findBySchoolIdAndFoodCode(Long schoolId, String foodCode);

    boolean existsByFoodCode(String foodCode);

    // 같은 학교 내에서 메뉴명 중복 체크
    boolean existsBySchoolIdAndFoodName(Long schoolId, String foodName);

    boolean existsByFoodName(String foodName);

    @Query(value = "SELECT MAX(CAST(SUBSTRING(food_code, 9) AS UNSIGNED)) FROM new_food_info WHERE food_code LIKE 'NEWFOOD-%'", nativeQuery = true)
    Integer findMaxFoodCodeNumber();

    // 학교별 삭제되지 않은 신메뉴 목록 (식단 생성용)
    List<NewFoodInfo> findBySchoolIdAndDeletedFalse(Long schoolId);

    List<NewFoodInfo> findByDeletedFalse();

    // 음식명으로 삭제되지 않은 신메뉴 조회
    Optional<NewFoodInfo> findByFoodNameAndDeletedFalse(String foodName);

    // 학교별 신메뉴 목록 (페이지네이션 - 고정 정렬)
    Page<NewFoodInfo> findBySchoolIdAndDeletedFalseOrderByCreatedAtDesc(Long schoolId, Pageable pageable);

    // 학교별 신메뉴 목록 (페이지네이션 - 동적 정렬)
    Page<NewFoodInfo> findBySchoolIdAndDeletedFalse(Long schoolId, Pageable pageable);

    // 학교별 + 카테고리 필터 신메뉴 목록 (페이지네이션 - 동적 정렬)
    Page<NewFoodInfo> findBySchoolIdAndCategoryAndDeletedFalse(Long schoolId, String category, Pageable pageable);

    Page<NewFoodInfo> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
}
