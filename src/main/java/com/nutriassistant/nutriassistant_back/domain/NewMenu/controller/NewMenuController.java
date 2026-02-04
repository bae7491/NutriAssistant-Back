package com.nutriassistant.nutriassistant_back.domain.NewMenu.controller;

import com.nutriassistant.nutriassistant_back.domain.Board.entity.Board;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.CategoryType;
import com.nutriassistant.nutriassistant_back.domain.Board.repository.BoardRepository;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewFoodInfoCreateRequest;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewFoodInfoDeleteResponse;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewFoodInfoResponse;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewFoodInfoUpdateRequest;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.DTO.NewMenuAnalysisResponse;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.service.NewMenuService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import com.nutriassistant.nutriassistant_back.global.auth.CurrentUser;
import com.nutriassistant.nutriassistant_back.global.auth.UserContext;
import com.nutriassistant.nutriassistant_back.global.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
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
     * @param user JWT 토큰에서 추출한 사용자 정보
     * @param boardId 게시글 ID
     * @return 분석 결과
     */
    @PostMapping("/new-menu/analyze/{boardId}")
    public ResponseEntity<?> analyzeNewMenu(
            @CurrentUser UserContext user,
            @PathVariable Long boardId) {
        log.info("🤖 신메뉴 분석 요청: schoolId={}, boardId={}", user.getSchoolId(), boardId);

        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.of(404, "NOT_FOUND", "BOARD_002",
                            "게시글을 찾을 수 없습니다: " + boardId, "/new-menu/analyze/" + boardId)
            );
        }

        // schoolId 검증: 본인 학교 게시글만 분석 가능
        if (!board.getSchoolId().equals(user.getSchoolId())) {
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
    @GetMapping("/new-menu/internal/feedback")
    public List<Board> getNewMenuFeedback(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "500") int size
    ) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return boardRepository.findByCategoryAndDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                CategoryType.NEW_MENU,
                since,
                PageRequest.of(0, size)
        );
    }

    /**
     * 신메뉴 목록 조회 (페이지네이션)
     */
    @GetMapping("/newfoodinfo")
    public ResponseEntity<ApiResponse<Page<NewFoodInfoResponse>>> getNewFoodInfoList(
            @CurrentUser UserContext user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("📋 신메뉴 목록 조회: schoolId={}, page={}, size={}", user.getSchoolId(), page, size);

        // page는 1부터 시작하므로 0-based로 변환
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), size);
        Page<NewFoodInfoResponse> result = newMenuService.getNewFoodInfoList(pageRequest, user.getSchoolId());

        return ResponseEntity.ok(
                ApiResponse.success("신메뉴 목록 조회 성공", result)
        );
    }

    /**
     * 신메뉴 상세 조회
     */
    @GetMapping("/newfoodinfo/{newFoodId}")
    public ResponseEntity<ApiResponse<NewFoodInfoResponse>> getNewFoodInfo(
            @CurrentUser UserContext user,
            @PathVariable String newFoodId
    ) {
        try {
            log.info("🔍 신메뉴 상세 조회: schoolId={}, newFoodId={}", user.getSchoolId(), newFoodId);

            NewFoodInfoResponse response = newMenuService.getNewFoodInfo(newFoodId, user.getSchoolId());

            return ResponseEntity.ok(
                    ApiResponse.success("신메뉴 조회 성공", response)
            );

        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("NOT_FOUND:")) {
                log.warn("⚠️ 신메뉴 없음: {}", newFoodId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error("해당 신메뉴를 찾을 수 없습니다.")
                );
            }
            throw e;
        }
    }

    /**
     * 신메뉴 직접 등록
     */
    @PostMapping("/newfoodinfo")
    public ResponseEntity<ApiResponse<NewFoodInfoResponse>> createNewFoodInfo(
            @CurrentUser UserContext user,
            @Validated @RequestBody NewFoodInfoCreateRequest request
    ) {
        try {
            log.info("📝 신메뉴 등록 요청: schoolId={}, name={}", user.getSchoolId(), request.getName());

            NewFoodInfoResponse response = newMenuService.createNewFoodInfo(request, user.getSchoolId());

            return ResponseEntity.ok(
                    ApiResponse.success("신메뉴가 생성되었습니다.", response)
            );

        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("DUPLICATE:")) {
                String menuName = e.getMessage().substring("DUPLICATE:".length());
                log.warn("⚠️ 중복 메뉴명: {}", menuName);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse.error("이미 존재하는 메뉴입니다.",
                                new ApiResponse.ErrorDetails("name", menuName))
                );
            }
            throw e;

        } catch (Exception e) {
            log.error("❌ 신메뉴 등록 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId))
            );
        }
    }

    /**
     * 신메뉴 수정
     */
    @PatchMapping("/newfoodinfo/{newMenuId}")
    public ResponseEntity<ApiResponse<NewFoodInfoResponse>> updateNewFoodInfo(
            @CurrentUser UserContext user,
            @PathVariable String newMenuId,
            @RequestBody NewFoodInfoUpdateRequest request
    ) {
        try {
            log.info("✏️ 신메뉴 수정 요청: schoolId={}, newMenuId={}", user.getSchoolId(), newMenuId);

            NewFoodInfoResponse response = newMenuService.updateNewFoodInfo(newMenuId, request, user.getSchoolId());

            return ResponseEntity.ok(
                    ApiResponse.success("신메뉴가 수정되었습니다.", response)
            );

        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("NOT_FOUND:")) {
                log.warn("⚠️ 신메뉴 없음: {}", newMenuId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error("해당 신메뉴를 찾을 수 없습니다.")
                );
            }
            throw e;

        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("DUPLICATE:")) {
                String menuName = e.getMessage().substring("DUPLICATE:".length());
                log.warn("⚠️ 중복 메뉴명: {}", menuName);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        ApiResponse.error("이미 존재하는 메뉴명입니다.",
                                new ApiResponse.ErrorDetails("name", "duplicate"))
                );
            }
            throw e;

        } catch (Exception e) {
            log.error("❌ 신메뉴 수정 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId))
            );
        }
    }

    /**
     * 신메뉴 삭제
     */
    @DeleteMapping("/newfoodinfo/{newFoodId}")
    public ResponseEntity<ApiResponse<NewFoodInfoDeleteResponse>> deleteNewFoodInfo(
            @CurrentUser UserContext user,
            @PathVariable String newFoodId
    ) {
        try {
            log.info("🗑️ 신메뉴 삭제 요청: schoolId={}, newFoodId={}", user.getSchoolId(), newFoodId);

            NewFoodInfoDeleteResponse response = newMenuService.deleteNewFoodInfo(newFoodId, user.getSchoolId());

            return ResponseEntity.ok(
                    ApiResponse.success("신메뉴가 삭제되었습니다.", response)
            );

        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("NOT_FOUND:")) {
                log.warn("⚠️ 신메뉴 없음: {}", newFoodId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error("해당 신메뉴를 찾을 수 없습니다.")
                );
            }
            throw e;

        } catch (Exception e) {
            log.error("❌ 신메뉴 삭제 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId))
            );
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String field = fieldError != null ? fieldError.getField() : "unknown";
        String reason = fieldError != null ? fieldError.getDefaultMessage() : "invalid";

        return ResponseEntity.badRequest().body(
                ApiResponse.error("요청값 검증에 실패했습니다.",
                        new ApiResponse.ErrorDetails(field, reason))
        );
    }
}
