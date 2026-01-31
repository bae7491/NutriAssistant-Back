package com.nutriassistant.nutriassistant_back.domain.NewMenu.controller;

import com.nutriassistant.nutriassistant_back.domain.Board.entity.Board;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.CategoryType;
import com.nutriassistant.nutriassistant_back.domain.Board.repository.BoardRepository;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewMenuAnalysisResponse;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.service.NewMenuService;
import com.nutriassistant.nutriassistant_back.global.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/new-menu")
public class NewMenuController {

    private final BoardRepository boardRepository;
    private final NewMenuService newMenuService;

    public NewMenuController(BoardRepository boardRepository, NewMenuService newMenuService) {
        this.boardRepository = boardRepository;
        this.newMenuService = newMenuService;
    }

    /**
     * 신메뉴 분석 요청
     * 특정 게시글에 대해 FastAPI를 통한 신메뉴 분석을 수행
     *
     * @param boardId 게시글 ID
     * @return 분석 결과
     */
    @PostMapping("/analyze/{boardId}")
    public ResponseEntity<?> analyzeNewMenu(@PathVariable Long boardId) {
        log.info("🤖 신메뉴 분석 요청: boardId={}", boardId);

        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.of(404, "NOT_FOUND", "BOARD_002",
                            "게시글을 찾을 수 없습니다: " + boardId, "/new-menu/analyze/" + boardId)
            );
        }

        if (board.getCategory() != CategoryType.NEW_MENU) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.of(400, "BAD_REQUEST", "BOARD_003",
                            "NEW_MENU 카테고리의 게시글만 분석할 수 있습니다.", "/new-menu/analyze/" + boardId)
            );
        }

        NewMenuAnalysisResponse response = newMenuService.requestAnalysis(board);

        if (response.getSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 신메뉴 요청 게시판 조회 (FastAPI용 - Internal API)
     *
     * @param days 조회 기간 (일)
     * @param size 조회 개수
     * @return 신메뉴 카테고리 게시글 리스트
     */
    @GetMapping("/internal/feedback")
    public List<Board> getNewMenuFeedback(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "500") int size
    ) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return boardRepository.findByCategoryAndCreatedAtAfterOrderByCreatedAtDesc(
                CategoryType.NEW_MENU,
                since,
                PageRequest.of(0, size)
        );
    }
}
