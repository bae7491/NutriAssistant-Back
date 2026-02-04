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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Board", description = "게시판 API - 게시글 CRUD 기능 제공")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @Operation(
            summary = "게시글 등록",
            description = "새 게시글을 등록합니다. NEW_MENU 카테고리인 경우 AI 분석이 자동으로 요청됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "게시글 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<?> createBoard(
            @CurrentUser UserContext user,
            @Validated @RequestBody BoardCreateRequest request
    ) {
        try {
            log.info("📝 게시글 등록 API 호출: category={}, title={}, userId={}, role={}, schoolId={}",
                    request.getCategory(), request.getTitle(), user.getUserId(), user.getRole(), user.getSchoolId());

            BoardCreateResponse response = boardService.createBoard(
                    request,
                    user.getSchoolId(),
                    user.getUserId(),
                    user.getRole()
            );

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

    @Operation(
            summary = "게시글 목록 조회",
            description = "게시글 목록을 페이징하여 조회합니다. 카테고리와 키워드로 필터링 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<BoardListResponse>> getBoardList(
            @CurrentUser UserContext user,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기 (최대 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "카테고리 필터 (NOTICE, NEW_MENU, FREE 등)")
            @RequestParam(required = false) String category,
            @Parameter(description = "검색 키워드 (제목, 작성자명)")
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

            log.info("📋 게시글 목록 조회: schoolId={}, page={}, size={}, category={}, keyword={}",
                    user.getSchoolId(), page, size, category, keyword);

            BoardListResponse response = boardService.getBoardList(user.getSchoolId(), category, keyword, page - 1, size);

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

    @Operation(
            summary = "게시글 상세 조회",
            description = "게시글의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "삭제된 게시글")
    })
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getBoardDetail(
            @CurrentUser UserContext user,
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long boardId
    ) {
        try {
            log.info("📖 게시글 상세 조회 API 호출: boardId={}, schoolId={}, userId={}", boardId, user.getSchoolId(), user.getUserId());

            BoardDetailResponse response = boardService.getBoardDetail(boardId, user.getSchoolId(), user.getUserId());

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

    @Operation(
            summary = "게시글 수정",
            description = "게시글을 수정합니다. 작성자 본인만 수정할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "삭제된 게시글")
    })
    @PatchMapping("/{boardId}")
    public ResponseEntity<?> updateBoard(
            @CurrentUser UserContext user,
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long boardId,
            @RequestBody BoardUpdateRequest request
    ) {
        String path = "/boards/" + boardId;
        try {
            log.info("✏️ 게시글 수정 API 호출: boardId={}, schoolId={}, userId={}", boardId, user.getSchoolId(), user.getUserId());

            BoardCreateResponse response = boardService.updateBoard(boardId, user.getSchoolId(), request, user.getUserId());

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

    @Operation(
            summary = "게시글 삭제",
            description = "게시글을 삭제합니다 (Soft Delete). 작성자 본인만 삭제할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @DeleteMapping("/{boardId}")
    public ResponseEntity<?> deleteBoard(
            @CurrentUser UserContext user,
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long boardId
    ) {
        String path = "/boards/" + boardId;
        try {
            log.info("🗑️ 게시글 삭제 API 호출: boardId={}, schoolId={}, userId={}", boardId, user.getSchoolId(), user.getUserId());

            BoardDeleteResponse response = boardService.deleteBoard(boardId, user.getSchoolId(), user.getUserId());

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
