package com.nutriassistant.nutriassistant_back.global.exception;

import com.nutriassistant.nutriassistant_back.domain.Board.service.BoardService;
import com.nutriassistant.nutriassistant_back.domain.File.service.FileService;
import com.nutriassistant.nutriassistant_back.global.aws.S3Uploader;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // File 관련 예외 처리
    @ExceptionHandler(FileService.FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(
            FileService.FileUploadException e, HttpServletRequest request) {
        log.error("파일 업로드 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        400,
                        "BAD_REQUEST",
                        e.getCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(FileService.FileTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLargeException(
            FileService.FileTooLargeException e, HttpServletRequest request) {
        log.error("파일 크기 초과: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(
                        413,
                        "PAYLOAD_TOO_LARGE",
                        e.getCode(),
                        e.getMessage(),
                        request.getRequestURI(),
                        Map.of("max_bytes", e.getMaxBytes())
                ));
    }

    @ExceptionHandler(FileService.FileRelatedNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileRelatedNotFoundException(
            FileService.FileRelatedNotFoundException e, HttpServletRequest request) {
        log.error("연결 대상 리소스 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        404,
                        "NOT_FOUND",
                        e.getCode(),
                        e.getMessage(),
                        request.getRequestURI(),
                        Map.of(
                                "related_type", e.getRelatedType(),
                                "related_id", e.getRelatedId()
                        )
                ));
    }

    @ExceptionHandler(FileService.FileForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleFileForbiddenException(
            FileService.FileForbiddenException e, HttpServletRequest request) {
        log.error("파일 업로드 권한 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        403,
                        "FORBIDDEN",
                        e.getCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(S3Uploader.S3UploadException.class)
    public ResponseEntity<ErrorResponse> handleS3UploadException(
            S3Uploader.S3UploadException e, HttpServletRequest request) {
        log.error("S3 업로드 오류: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        500,
                        "INTERNAL_SERVER_ERROR",
                        "SYS_001",
                        "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.error("Spring 파일 크기 초과: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(
                        413,
                        "PAYLOAD_TOO_LARGE",
                        "FILE_413",
                        "파일 용량이 제한(10MB)을 초과했습니다.",
                        request.getRequestURI(),
                        Map.of("max_bytes", 10485760L)
                ));
    }

    // Board 관련 예외 처리
    @ExceptionHandler(BoardService.BoardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBoardNotFoundException(
            BoardService.BoardNotFoundException e, HttpServletRequest request) {
        log.error("게시글 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        404,
                        "NOT_FOUND",
                        "BOARD_404",
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(BoardService.BoardDeletedException.class)
    public ResponseEntity<ErrorResponse> handleBoardDeletedException(
            BoardService.BoardDeletedException e, HttpServletRequest request) {
        log.error("삭제된 게시글: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        404,
                        "NOT_FOUND",
                        "BOARD_404",
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(BoardService.BoardForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleBoardForbiddenException(
            BoardService.BoardForbiddenException e, HttpServletRequest request) {
        log.error("게시글 권한 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        403,
                        "FORBIDDEN",
                        "AUTH_103",
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    // 일반 예외 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        log.error("잘못된 인자: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        400,
                        "BAD_REQUEST",
                        "REQ_001",
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    // 모든 예외 처리 (디버깅용)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {
        log.error("서버 오류 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        500,
                        "INTERNAL_SERVER_ERROR",
                        "SYS_001",
                        "서버 내부 오류: " + e.getMessage(),
                        request.getRequestURI()
                ));
    }
}
