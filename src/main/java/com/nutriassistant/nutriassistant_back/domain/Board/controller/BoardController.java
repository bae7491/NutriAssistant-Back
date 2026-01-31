package com.nutriassistant.nutriassistant_back.domain.Board.controller;

import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardCreateRequest;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardCreateResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.service.BoardService;
import com.nutriassistant.nutriassistant_back.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /**
     * 게시글 등록
     * NEW_MENU 카테고리인 경우 자동으로 FastAPI에 신메뉴 분석 요청
     */
    @PostMapping
    public ResponseEntity<?> createBoard(
            @Validated @RequestBody BoardCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            // TODO: JWT에서 schoolId 추출
            Long schoolId = extractSchoolId();

            log.info("📝 게시글 등록 API 호출: category={}, title={}, authorId={}, authorType={}",
                    request.getCategory(), request.getTitle(), request.getAuthorId(), request.getAuthorType());

            BoardCreateResponse response = boardService.createBoard(request, schoolId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ErrorResponse.of(
                            400,
                            "BAD_REQUEST",
                            "BOARD_001",
                            e.getMessage(),
                            "/boards"
                    )
            );

        } catch (Exception e) {
            log.error("❌ 게시글 등록 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.of(
                            500,
                            "INTERNAL_SERVER_ERROR",
                            "SYS_001",
                            "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                            "/boards"
                    )
            );
        }
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "요청값 검증에 실패했습니다.";

        log.warn("⚠️ 검증 실패: {}", message);

        return ResponseEntity.badRequest().body(
                ErrorResponse.of(
                        400,
                        "BAD_REQUEST",
                        "BOARD_001",
                        message,
                        "/boards"
                )
        );
    }

    // TODO: JWT 구현 시 실제 토큰에서 추출하도록 수정
    private Long extractSchoolId() {
        return 1L;
    }
}
