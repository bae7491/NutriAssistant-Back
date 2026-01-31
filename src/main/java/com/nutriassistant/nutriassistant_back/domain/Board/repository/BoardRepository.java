package com.nutriassistant.nutriassistant_back.domain.Board.repository;

import com.nutriassistant.nutriassistant_back.domain.Board.entity.Board;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.CategoryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByCategoryAndCreatedAtAfterOrderByCreatedAtDesc(
            CategoryType category, LocalDateTime since, Pageable pageable);
}
