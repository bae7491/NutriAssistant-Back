package com.nutriassistant.nutriassistant_back.domain.Board.controller;

import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardCreateRequest;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardCreateResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardDeleteResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardDetailResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardListResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.Board.service.BoardService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import com.nutriassistant.nutriassistant_back.global.auth.CurrentUser;
import com.nutriassistant.nutriassistant_back.global.auth.UserContext;
import com.nutriassistant.nutriassistant_back.global.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
            @CurrentUser UserContext user,
            @Validated @RequestBody BoardCreateRequest request
    ) {
        try {
            log.info("📝 게시글 등록 API 호출: category={}, title={}, authorId={}, authorType={}, schoolId={}",
                    request.getCategory(), request.getTitle(), request.getAuthorId(), request.getAuthorType(), user.getSchoolId());

            BoardCreateResponse response = boardService.createBoard(request, user.getSchoolId());

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

    /**
     * 게시글 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<BoardListResponse>> getBoardList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        try {
            if (size > 100) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("요청 파라미터가 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("size", "max 100"))
                );
            }

            if (page < 1) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("요청 파라미터가 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("page", "min 1"))
                );
            }

            log.info("📋 게시글 목록 조회: page={}, size={}, category={}, keyword={}",
                    page, size, category, keyword);

            BoardListResponse response = boardService.getBoardList(category, keyword, page - 1, size);

            return ResponseEntity.ok(
                    ApiResponse.success("게시글 목록 조회 성공", response)
            );

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage())
            );

        } catch (Exception e) {
            log.error("❌ 게시글 목록 조회 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId))
            );
        }
    }

    /**
     * 게시글 상세 조회
     */
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getBoardDetail(
            @CurrentUser UserContext user,
            @PathVariable Long boardId
    ) {
        try {
            log.info("📖 게시글 상세 조회 API 호출: boardId={}, userId={}", boardId, user.getUserId());

            BoardDetailResponse response = boardService.getBoardDetail(boardId, user.getUserId());

            return ResponseEntity.ok(
                    ApiResponse.success("게시글 상세 조회 성공", response)
            );

        } catch (BoardService.BoardNotFoundException e) {
            log.warn("⚠️ 게시글 없음: boardId={}", boardId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(e.getMessage())
            );

        } catch (BoardService.BoardDeletedException e) {
            log.warn("⚠️ 삭제된 게시글: boardId={}", boardId);
            return ResponseEntity.status(HttpStatus.GONE).body(
                    ApiResponse.error(e.getMessage())
            );

        } catch (Exception e) {
            log.error("❌ 게시글 상세 조회 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId))
            );
        }
    }

    /**
     * 게시글 수정
     */
    @PatchMapping("/{boardId}")
    public ResponseEntity<?> updateBoard(
            @CurrentUser UserContext user,
            @PathVariable Long boardId,
            @RequestBody BoardUpdateRequest request
    ) {
        String path = "/boards/" + boardId;
        try {
            log.info("✏️ 게시글 수정 API 호출: boardId={}, userId={}", boardId, user.getUserId());

            BoardCreateResponse response = boardService.updateBoard(boardId, request, user.getUserId());

            return ResponseEntity.ok(response);

        } catch (BoardService.BoardNotFoundException e) {
            log.warn("⚠️ 게시글 없음: boardId={}", boardId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponse.of(
                            404,
                            "NOT_FOUND",
                            "BOARD_404",
                            e.getMessage(),
                            path
                    )
            );

        } catch (BoardService.BoardDeletedException e) {
            log.warn("⚠️ 삭제된 게시글: boardId={}", boardId);
            return ResponseEntity.status(HttpStatus.GONE).body(
                    ErrorResponse.of(
                            410,
                            "GONE",
                            "BOARD_410",
                            e.getMessage(),
                            path
                    )
            );

        } catch (BoardService.BoardForbiddenException e) {
            log.warn("⚠️ 수정 권한 없음: boardId={}", boardId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ErrorResponse.of(
                            403,
                            "FORBIDDEN",
                            "AUTH_101",
                            e.getMessage(),
                            path
                    )
            );

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ErrorResponse.of(
                            400,
                            "BAD_REQUEST",
                            "BOARD_002",
                            e.getMessage(),
                            path
                    )
            );

        } catch (Exception e) {
            log.error("❌ 게시글 수정 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.of(
                            500,
                            "INTERNAL_SERVER_ERROR",
                            "SYS_001",
                            "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                            path
                    )
            );
        }
    }

    /**
     * 게시글 삭제 (Soft Delete)
     */
    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(
            @CurrentUser UserContext user,
            @PathVariable Long boardId
    ) {
        String path = "/boards/" + boardId;
        try {
            log.info("🗑️ 게시글 삭제 API 호출: boardId={}, userId={}", boardId, user.getUserId());

            BoardDeleteResponse response = boardService.deleteBoard(boardId, user.getUserId());

            return ResponseEntity.ok(
                    ApiResponse.success("게시글이 삭제되었습니다.", response)
            );

        } catch (BoardService.BoardNotFoundException e) {
            log.warn("⚠️ 게시글 없음: boardId={}", boardId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponse.of(
                            404,
                            "NOT_FOUND",
                            "BOARD_404",
                            e.getMessage(),
                            path
                    )
            );

        } catch (BoardService.BoardForbiddenException e) {
            log.warn("⚠️ 삭제 권한 없음: boardId={}", boardId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ErrorResponse.of(
                            403,
                            "FORBIDDEN",
                            "AUTH_102",
                            e.getMessage(),
                            path
                    )
            );

        } catch (Exception e) {
            log.error("❌ 게시글 삭제 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponse.of(
                            500,
                            "INTERNAL_SERVER_ERROR",
                            "SYS_001",
                            "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                            path
                    )
            );
        }
    }
}
