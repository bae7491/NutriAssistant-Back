package com.nutriassistant.nutriassistant_back.domain.Board.repository;

import com.nutriassistant.nutriassistant_back.domain.Board.entity.Board;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.CategoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByCategoryAndDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            CategoryType category, LocalDateTime since, Pageable pageable);

    // 카테고리 + 키워드 검색 (제목 또는 작성자명) - 삭제되지 않은 게시글만 조회 + school_id 필터링
    @Query("SELECT b FROM Board b WHERE " +
            "b.deleted = false AND " +
            "b.schoolId = :schoolId AND " +
            "(:category IS NULL OR b.category = :category) AND " +
            "(:keyword IS NULL OR :keyword = '' OR b.title LIKE %:keyword% OR b.authorName LIKE %:keyword%) " +
            "ORDER BY b.createdAt DESC")
    Page<Board> findByFilters(
            @Param("schoolId") Long schoolId,
            @Param("category") CategoryType category,
            @Param("keyword") String keyword,
            Pageable pageable);
}
