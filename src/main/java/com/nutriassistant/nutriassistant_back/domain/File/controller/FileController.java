package com.nutriassistant.nutriassistant_back.domain.File.controller;

import com.nutriassistant.nutriassistant_back.domain.File.dto.FileUploadResponse;
import com.nutriassistant.nutriassistant_back.domain.File.service.FileService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import com.nutriassistant.nutriassistant_back.global.auth.CurrentUser;
import com.nutriassistant.nutriassistant_back.global.auth.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File", description = "파일 업로드 API")
public class FileController {

    private final FileService fileService;

    @Operation(summary = "파일 업로드", description = "게시판 등에 첨부할 파일을 S3에 업로드합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "파일 업로드 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청값 오류 (파일 누락 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "연결 대상 리소스 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "413",
                    description = "파일 용량 초과 (10MB)"
            )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @Parameter(description = "업로드할 파일", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "연결 대상 타입 (BOARD, COMMENT, REPORT)", required = true)
            @RequestParam("relatedType") String relatedType,

            @Parameter(description = "연결 대상 ID", required = true)
            @RequestParam("relatedId") Long relatedId,

            @CurrentUser UserContext userContext
    ) {
        log.info("파일 업로드 API 호출: relatedType={}, relatedId={}", relatedType, relatedId);

        // 인증 검증
        if (userContext == null || userContext.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("로그인이 필요합니다."));
        }

        FileUploadResponse response = fileService.uploadFile(
                file,
                relatedType,
                relatedId,
                userContext.getSchoolId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("파일 업로드 성공", response));
    }
}
