package com.nutriassistant.nutriassistant_back.domain.Board.service;

import com.nutriassistant.nutriassistant_back.domain.Attachment.entity.Attachment;
import com.nutriassistant.nutriassistant_back.domain.Attachment.entity.RelatedType;
import com.nutriassistant.nutriassistant_back.domain.Attachment.repository.AttachmentRepository;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardCreateRequest;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardCreateResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.AuthorType;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.Board;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.CategoryType;
import com.nutriassistant.nutriassistant_back.domain.Board.repository.BoardRepository;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.service.NewMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final AttachmentRepository attachmentRepository;
    private final NewMenuService newMenuService;

    public BoardService(BoardRepository boardRepository,
                        AttachmentRepository attachmentRepository,
                        NewMenuService newMenuService) {
        this.boardRepository = boardRepository;
        this.attachmentRepository = attachmentRepository;
        this.newMenuService = newMenuService;
    }

    @Transactional
    public BoardCreateResponse createBoard(BoardCreateRequest request, Long schoolId) {
        log.info("📝 게시글 등록 요청: category={}, title={}", request.getCategory(), request.getTitle());

        // 1. 카테고리 파싱
        CategoryType category;
        try {
            category = CategoryType.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + request.getCategory());
        }

        // 2. 작성자 타입 파싱
        AuthorType authorType;
        try {
            authorType = AuthorType.valueOf(request.getAuthorType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 작성자 유형입니다: " + request.getAuthorType());
        }

        // 3. 게시글 저장
        Board board = new Board(
                schoolId,
                category,
                request.getTitle(),
                request.getContent(),
                request.getAuthorId(),
                authorType
        );
        Board savedBoard = boardRepository.save(board);
        log.info("✅ 게시글 저장 완료: id={}", savedBoard.getId());

        // 4. 첨부파일 저장
        List<Attachment> savedAttachments = new ArrayList<>();
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (BoardCreateRequest.AttachmentRequest attachmentReq : request.getAttachments()) {
                // S3 경로 업데이트 (tmp -> 실제 경로)
                String finalS3Path = updateS3Path(attachmentReq.getS3Path(), schoolId, savedBoard.getId());

                Attachment attachment = new Attachment(
                        RelatedType.BOARD,
                        savedBoard.getId(),
                        attachmentReq.getFileName(),
                        finalS3Path,
                        attachmentReq.getFileType()
                );
                savedAttachments.add(attachmentRepository.save(attachment));
            }
            log.info("✅ 첨부파일 저장 완료: {}개", savedAttachments.size());
        }

        // 5. NEW_MENU 카테고리인 경우 비동기로 분석 요청
        if (category == CategoryType.NEW_MENU) {
            newMenuService.requestAnalysisAsync(savedBoard);
            log.info("🔄 신메뉴 분석 비동기 요청 전송: boardId={}", savedBoard.getId());
        }

        // 6. 응답 생성
        List<BoardCreateResponse.AttachmentResponse> attachmentResponses = savedAttachments.stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());

        return BoardCreateResponse.builder()
                .id(savedBoard.getId())
                .schoolId(savedBoard.getSchoolId())
                .category(savedBoard.getCategory().name())
                .title(savedBoard.getTitle())
                .content(savedBoard.getContent())
                .authorId(savedBoard.getAuthorId())
                .authorType(savedBoard.getAuthorType().name())
                .viewCount(savedBoard.getViewCount())
                .attachments(attachmentResponses)
                .createdAt(savedBoard.getCreatedAt())
                .updatedAt(savedBoard.getUpdatedAt())
                .build();
    }

    private BoardCreateResponse.AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return BoardCreateResponse.AttachmentResponse.builder()
                .id(attachment.getId())
                .relatedType(attachment.getRelatedType().name())
                .relatedId(attachment.getRelatedId())
                .fileName(attachment.getFileName())
                .s3Path(attachment.getS3Path())
                .fileType(attachment.getFileType())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    /**
     * 임시 S3 경로를 실제 경로로 변경
     * 예: schools/1/boards/tmp/9f2a.../menu.png -> schools/1/boards/101/menu.png
     */
    private String updateS3Path(String tmpPath, Long schoolId, Long boardId) {
        if (tmpPath == null || !tmpPath.contains("/tmp/")) {
            return tmpPath;
        }
        // tmp 경로에서 파일명 추출
        String fileName = tmpPath.substring(tmpPath.lastIndexOf("/") + 1);
        return String.format("schools/%d/boards/%d/%s", schoolId, boardId, fileName);
    }
}
